package com.community.healthcare.pharmacy.infrastructure;

import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * R3 处方、药房、收费和模拟医保的事务编排服务。
 *
 * <p>服务确保处方只能来源于已签署接诊，发药按近效期优先原则原子扣减批次库存；
 * 签署、审方、发药、支付和医保申报等关键操作均支持幂等重放并写入审计。</p>
 */
@Service
public class R3PharmacyBillingService {
    private final JdbcTemplate jdbc;
    private final AuditTrail audit;

    public R3PharmacyBillingService(JdbcTemplate jdbc, AuditTrail audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    /** 为医生本人已签署的接诊创建处方草稿。 */
    @Transactional
    public PrescriptionView createPrescription(long doctorId, PrescriptionCommand command) {
        requireText(command.diagnosis(), "诊断不能为空");
        if (command.items() == null || command.items().isEmpty()) throw new IllegalArgumentException("处方至少包含一条药品明细");
        Map<String, Object> encounter = single("select patient_id, staff_profile_id, status from clinical_encounter where id=?",
                command.encounterId());
        if (!"SIGNED".equals(encounter.get("status"))) throw new R3ConflictException("仅已签署接诊可开立处方");
        if (((Number) encounter.get("staff_profile_id")).longValue() != doctorId) throw new EntityNotFoundException("接诊记录不存在");
        long patientId = ((Number) encounter.get("patient_id")).longValue();
        long id = insert("insert into rx_prescription(encounter_id,patient_id,prescribed_by_staff_id,diagnosis,status,created_at,updated_at,version) values(?,?,?,?,'DRAFT',current_timestamp,current_timestamp,0)",
                command.encounterId(), patientId, doctorId, command.diagnosis().trim());
        for (PrescriptionItemCommand item : command.items()) {
            if (item.quantity() <= 0) throw new IllegalArgumentException("处方数量必须大于零");
            requireText(item.dosage(), "用法用量不能为空");
            if (count("select count(*) from rx_medicine_sku where id=? and active=true", item.skuId()) == 0) throw new EntityNotFoundException("药品不存在");
            jdbc.update("insert into rx_prescription_item(prescription_id,sku_id,quantity,dosage,created_at) values(?,?,?,?,current_timestamp)",
                    id, item.skuId(), item.quantity(), item.dosage().trim());
        }
        return prescription(id);
    }

    /** 幂等签署处方并提交药师审方。 */
    @Transactional
    public PrescriptionView sign(long doctorId, long id, String key, String actor) {
        requireKey(key);
        String requestHash = requestHash("{}");
        if (isReplay("RX_SIGN", actor, key, id, requestHash)) return prescription(id);
        Map<String, Object> row = prescriptionRow(id);
        if (((Number) row.get("prescribed_by_staff_id")).longValue() != doctorId) throw new EntityNotFoundException("处方不存在");
        requireStatus(row, "DRAFT", "当前处方不能签署");
        jdbc.update("update rx_prescription set status='PENDING_REVIEW',signed_at=current_timestamp,updated_at=current_timestamp,version=version+1 where id=?", id);
        remember("RX_SIGN", actor, key, id, requestHash);
        return prescription(id);
    }

    /** 药师幂等完成审方，记录通过或拒绝及意见。 */
    @Transactional
    public PrescriptionView review(long pharmacistId, long id, String key, ReviewCommand command, String actor) {
        requireKey(key);
        String requestHash = requestHash("approved=" + command.approved() + "&note=" + normalize(command.note()));
        if (isReplay("RX_REVIEW", actor, key, id, requestHash)) return prescription(id);
        Map<String, Object> row = prescriptionRow(id);
        requireStatus(row, "PENDING_REVIEW", "当前处方不能审方");
        String decision = command.approved() ? "APPROVED" : "REJECTED";
        if (!command.approved()) requireText(command.note(), "拒绝原因不能为空");
        jdbc.update("insert into rx_review(prescription_id,pharmacist_staff_id,decision,note,reviewed_at) values(?,?,?,?,current_timestamp)",
                id, pharmacistId, decision, command.note());
        jdbc.update("update rx_prescription set status=?,updated_at=current_timestamp,version=version+1 where id=?", decision, id);
        remember("RX_REVIEW", actor, key, id, requestHash);
        appendAudit(actor, "PHARMACIST", "PRESCRIPTION_REVIEWED", "PRESCRIPTION", id,
                "{\"decision\":\"" + decision + "\"}");
        return prescription(id);
    }

    /** 药师开始配药。 */
    @Transactional
    public PrescriptionView pick(long pharmacistId, long id) {
        requireStatus(prescriptionRow(id), "APPROVED", "当前处方不能配药");
        jdbc.update("insert into rx_dispense(prescription_id,pharmacist_staff_id,status,created_at,updated_at,version) values(?,?,'PICKING',current_timestamp,current_timestamp,0)", id, pharmacistId);
        changePrescriptionStatus(id, "PICKING");
        return prescription(id);
    }

    /** 由不同于配药人的药师执行双人核对。 */
    @Transactional
    public PrescriptionView check(long pharmacistId, long id) {
        requireStatus(prescriptionRow(id), "PICKING", "当前处方不能核对");
        int changed = jdbc.update("update rx_dispense set status='CHECKED',checked_at=current_timestamp,updated_at=current_timestamp,version=version+1 where prescription_id=?", id);
        if (changed != 1) throw new R3ConflictException("配药记录不存在");
        changePrescriptionStatus(id, "CHECKED");
        return prescription(id);
    }

    /**
     * 幂等执行发药，并按近效期优先规则逐批原子扣减库存。
     *
     * <p>任一药品库存不足都会回滚整个事务，不产生部分发药。</p>
     */
    @Transactional
    public PrescriptionView dispense(long pharmacistId, long id, String key, String actor) {
        requireKey(key);
        String requestHash = requestHash("{}");
        if (isReplay("RX_DISPENSE", actor, key, id, requestHash)) return prescription(id);
        requireStatus(prescriptionRow(id), "CHECKED", "当前处方不能发药");
        long dispenseId = jdbc.queryForObject("select id from rx_dispense where prescription_id=?", Long.class, id);
        List<Map<String, Object>> items = jdbc.queryForList("select id,sku_id,quantity from rx_prescription_item where prescription_id=? order by id", id);
        for (Map<String, Object> item : items) {
            long itemId = number(item, "id"), skuId = number(item, "sku_id");
            int remaining = ((Number) item.get("quantity")).intValue();
            List<Map<String, Object>> batches = jdbc.queryForList("select id,quantity_on_hand,version from inventory_batch where sku_id=? and quantity_on_hand>0 and expires_on>? order by expires_on,id for update", skuId, LocalDate.now());
            int available = batches.stream().mapToInt(b -> ((Number) b.get("quantity_on_hand")).intValue()).sum();
            if (available < remaining) throw new R3ConflictException("药品库存不足，不能发药");
            for (Map<String, Object> batch : batches) {
                if (remaining == 0) break;
                long batchId = number(batch, "id");
                int before = ((Number) batch.get("quantity_on_hand")).intValue();
                int issued = Math.min(before, remaining), after = before - issued;
                int changed = jdbc.update("update inventory_batch set quantity_on_hand=?,updated_at=current_timestamp,version=version+1 where id=? and version=? and quantity_on_hand=?",
                        after, batchId, number(batch, "version"), before);
                if (changed != 1) throw new R3ConflictException("库存已变化，请重试");
                jdbc.update("insert into inventory_transaction(batch_id,transaction_type,quantity,balance_after,reference_type,reference_id,actor_staff_id,occurred_at) values(?,'ISSUE',?,?, 'PRESCRIPTION',?,?,current_timestamp)",
                        batchId, -issued, after, String.valueOf(id), pharmacistId);
                jdbc.update("insert into rx_dispense_item(dispense_id,prescription_item_id,batch_id,quantity) values(?,?,?,?)",
                        dispenseId, itemId, batchId, issued);
                remaining -= issued;
            }
        }
        jdbc.update("update rx_dispense set status='DISPENSED',dispensed_at=current_timestamp,updated_at=current_timestamp,version=version+1 where id=?", dispenseId);
        changePrescriptionStatus(id, "DISPENSED");
        remember("RX_DISPENSE", actor, key, id, requestHash);
        appendAudit(actor, "PHARMACIST", "PRESCRIPTION_DISPENSED", "PRESCRIPTION", id, "{\"fefo\":true}");
        return prescription(id);
    }

    /** 创建账单和不可变收费明细快照。 */
    @Transactional
    public InvoiceView createInvoice(long registrarId, InvoiceCommand command) {
        if (command.lines() == null || command.lines().isEmpty()) throw new IllegalArgumentException("账单至少包含一条费用");
        BigDecimal total = command.lines().stream().map(l -> money(l.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long invoiceId = insert("insert into billing_invoice(patient_id,prescription_id,status,total_amount,paid_amount,refunded_amount,created_at,updated_at,version) values(?,?,'DRAFT',?,0,0,current_timestamp,current_timestamp,0)",
                command.patientId(), command.prescriptionId(), total);
        for (InvoiceLineCommand line : command.lines()) {
            requireText(line.description(), "费用名称不能为空");
            BigDecimal amount = money(line.amount());
            long chargeId = insert("insert into billing_charge_item(patient_id,prescription_id,description,amount,status,created_at) values(?,?,?,?,'BILLED',current_timestamp)",
                    command.patientId(), command.prescriptionId(), line.description().trim(), amount);
            jdbc.update("insert into billing_invoice_line(invoice_id,charge_item_id,description,amount) values(?,?,?,?)",
                    invoiceId, chargeId, line.description().trim(), amount);
        }
        return invoice(invoiceId);
    }

    @Transactional
    public InvoiceView issue(long id) {
        requireStatus(invoiceRow(id), "DRAFT", "当前账单不能出账");
        jdbc.update("update billing_invoice set status='ISSUED',issued_at=current_timestamp,updated_at=current_timestamp,version=version+1 where id=?", id);
        return invoice(id);
    }

    /** 幂等登记支付并更新账单累计实付金额。 */
    @Transactional
    public InvoiceView pay(long id, String key, PaymentCommand command, String actor) {
        requireKey(key);
        BigDecimal amount = money(command.amount());
        String channel = requireText(command.channel(), "支付渠道不能为空");
        String requestHash = requestHash("amount=" + amount.toPlainString() + "&channel=" + channel);
        if (isReplay("BILLING_PAY", actor, key, id, requestHash)) return invoice(id);
        int changed = jdbc.update("update billing_invoice set paid_amount=paid_amount+?,status=case when paid_amount+?=total_amount then 'PAID' else 'PARTIALLY_PAID' end,updated_at=current_timestamp,version=version+1 where id=? and status in ('ISSUED','PARTIALLY_PAID') and paid_amount+?<=total_amount",
                amount, amount, id, amount);
        if (changed != 1) throw new R3ConflictException("账单状态已变化或支付金额超额");
        jdbc.update("insert into billing_payment(invoice_id,amount,channel,paid_at,idempotency_key) values(?,?,?,current_timestamp,?)",
                id, amount, channel, storedKey("BILLING_PAY", actor, key, id));
        remember("BILLING_PAY", actor, key, id, requestHash);
        return invoice(id);
    }

    /** 幂等登记退款，拒绝超过实付金额的累计退款。 */
    @Transactional
    public InvoiceView refund(long registrarId, long id, String key, RefundCommand command, String actor) {
        requireKey(key);
        BigDecimal amount = money(command.amount());
        String reason = requireText(command.reason(), "退款原因不能为空");
        String requestHash = requestHash("amount=" + amount.toPlainString() + "&reason=" + reason);
        if (isReplay("BILLING_REFUND", actor, key, id, requestHash)) return invoice(id);
        int changed = jdbc.update("update billing_invoice set refunded_amount=refunded_amount+?,status=case when refunded_amount+?=paid_amount then 'REFUNDED' else 'PAID' end,updated_at=current_timestamp,version=version+1 where id=? and status='PAID' and refunded_amount+?<=paid_amount",
                amount, amount, id, amount);
        if (changed != 1) throw new R3ConflictException("账单状态已变化或退款金额超额");
        jdbc.update("insert into billing_refund(invoice_id,amount,reason,refunded_by_staff_id,refunded_at,idempotency_key) values(?,?,?,?,current_timestamp,?)",
                id, amount, reason, registrarId, storedKey("BILLING_REFUND", actor, key, id));
        remember("BILLING_REFUND", actor, key, id, requestHash);
        appendAudit(actor, "REGISTRAR", "BILLING_REFUNDED", "INVOICE", id, "{\"amount\":\"" + amount + "\"}");
        return invoice(id);
    }

    /** 仅按居民标识查询其本人账单。 */
    @Transactional(readOnly = true)
    public List<InvoiceView> residentInvoices(long patientId) {
        return jdbc.queryForList("select id from billing_invoice where patient_id=? order by created_at desc", patientId)
                .stream().map(r -> invoice(number(r, "id"))).toList();
    }

    /** 返回显式标记为模拟结果的医保资格检查。 */
    public EligibilityView eligibility(long patientId) {
        if (count("select count(*) from patient where id=? and active=true", patientId) == 0) throw new EntityNotFoundException("居民不存在");
        return new EligibilityView(patientId, true, true, "SIMULATED_ELIGIBLE");
    }

    /** 幂等创建并模拟受理医保申报。 */
    @Transactional
    public ClaimView submitClaim(String key, ClaimCommand command, String actor) {
        requireKey(key);
        BigDecimal amount = money(command.amount());
        String requestHash = requestHash("invoice=" + command.invoiceId() + "&amount=" + amount.toPlainString());
        Optional<Long> replay = replayResource("INSURANCE_CLAIM_SUBMIT", actor, key, command.invoiceId(), requestHash);
        if (replay.isPresent()) return claim(replay.get());
        Map<String, Object> inv = invoiceRow(command.invoiceId());
        if (amount.compareTo(decimal(inv, "total_amount")) > 0) throw new IllegalArgumentException("申报金额不能超过账单金额");
        long claimId = insert("insert into insurance_claim(invoice_id,patient_id,claimed_amount,status,simulation,created_at,updated_at,version) values(?,?,?,'SUBMITTED',true,current_timestamp,current_timestamp,0)",
                command.invoiceId(), number(inv, "patient_id"), amount);
        jdbc.update("insert into insurance_claim_event(claim_id,event_type,payload_json,occurred_at,idempotency_key) values(?,'SUBMITTED','{\"simulation\":true}',current_timestamp,?)", claimId, storedKey("INSURANCE_CLAIM_SUBMIT", actor, key, command.invoiceId()));
        remember("INSURANCE_CLAIM_SUBMIT", actor, key, command.invoiceId(), requestHash, claimId);
        appendAudit(actor, "REGISTRAR", "INSURANCE_CLAIM_SUBMITTED", "INSURANCE_CLAIM", claimId, "{\"simulation\":true}");
        return claim(claimId);
    }

    @Transactional
    public ClaimView settleClaim(long id, BigDecimal amount, String actor) {
        requireStatus(claimRow(id), "SUBMITTED", "当前医保申报不能结算");
        BigDecimal settled = money(amount);
        jdbc.update("update insurance_claim set status='SETTLED',settled_amount=?,external_reference=?,updated_at=current_timestamp,version=version+1 where id=?",
                settled, "SIM-" + UUID.randomUUID(), id);
        jdbc.update("insert into insurance_claim_event(claim_id,event_type,payload_json,occurred_at) values(?,'SETTLED','{\"simulation\":true}',current_timestamp)", id);
        return claim(id);
    }

    @Transactional
    public ClaimView cancelClaim(long id, String actor) {
        Map<String, Object> row = claimRow(id);
        if (!List.of("SUBMITTED", "SETTLED").contains(String.valueOf(row.get("status")))) throw new R3ConflictException("当前医保申报不能撤销");
        jdbc.update("update insurance_claim set status='CANCELLED',updated_at=current_timestamp,version=version+1 where id=?", id);
        jdbc.update("insert into insurance_claim_event(claim_id,event_type,payload_json,occurred_at) values(?,'CANCELLED','{\"simulation\":true}',current_timestamp)", id);
        return claim(id);
    }

    private PrescriptionView prescription(long id) {
        Map<String, Object> row = prescriptionRow(id);
        List<PrescriptionItemView> items = jdbc.query("select i.id,i.sku_id,s.name,i.quantity,i.dosage from rx_prescription_item i join rx_medicine_sku s on s.id=i.sku_id where i.prescription_id=? order by i.id",
                (rs, n) -> new PrescriptionItemView(rs.getLong("id"), rs.getLong("sku_id"), rs.getString("name"), rs.getInt("quantity"), rs.getString("dosage")), id);
        return new PrescriptionView(id, number(row,"encounter_id"), number(row,"patient_id"), String.valueOf(row.get("diagnosis")), String.valueOf(row.get("status")), items);
    }

    private InvoiceView invoice(long id) {
        Map<String, Object> row = invoiceRow(id);
        return new InvoiceView(id, number(row,"patient_id"), (Long) asLong(row.get("prescription_id")), String.valueOf(row.get("status")),
                decimal(row,"total_amount"), decimal(row,"paid_amount"), decimal(row,"refunded_amount"));
    }
    private ClaimView claim(long id) { Map<String,Object> r=claimRow(id); return new ClaimView(id,number(r,"invoice_id"),String.valueOf(r.get("status")),decimal(r,"claimed_amount"),(BigDecimal)r.get("settled_amount"),true,(String)r.get("external_reference")); }
    private Map<String,Object> prescriptionRow(long id){return single("select * from rx_prescription where id=?",id);}
    private Map<String,Object> invoiceRow(long id){return single("select * from billing_invoice where id=?",id);}
    private Map<String,Object> claimRow(long id){return single("select * from insurance_claim where id=?",id);}
    private Map<String,Object> single(String sql,Object...args){List<Map<String,Object>> rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new EntityNotFoundException("记录不存在");return rows.get(0);}
    private void requireStatus(Map<String,Object> row,String expected,String message){if(!expected.equals(String.valueOf(row.get("status"))))throw new R3ConflictException(message);}
    private void changePrescriptionStatus(long id,String status){jdbc.update("update rx_prescription set status=?,updated_at=current_timestamp,version=version+1 where id=?",status,id);}
    private long count(String sql,Object...args){return Objects.requireNonNull(jdbc.queryForObject(sql,Long.class,args));}
    private long insert(String sql,Object...args){KeyHolder kh=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);return ps;},kh);return Objects.requireNonNull(kh.getKey()).longValue();}
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static Long asLong(Object v){return v==null?null:((Number)v).longValue();}
    private static BigDecimal decimal(Map<String,Object> row,String key){Object v=row.get(key);return v instanceof BigDecimal b?b:new BigDecimal(String.valueOf(v));}
    private static BigDecimal money(BigDecimal v){if(v==null)throw new IllegalArgumentException("金额不能为空");try{v=v.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException e){throw new IllegalArgumentException("金额最多两位小数");}if(v.signum()<=0)throw new IllegalArgumentException("金额必须大于零");return v;}
    private static String requireText(String v,String message){if(v==null||v.isBlank())throw new IllegalArgumentException(message);return v.trim();}
    private static void requireKey(String key){if(key==null||key.isBlank()||key.length()>128)throw new IllegalArgumentException("Idempotency-Key 必填且不超过128字符");}
    private boolean isReplay(String scope,String actor,String key,long resourceId,String requestHash){return replayResource(scope,actor,key,resourceId,requestHash).isPresent();}
    private Optional<Long> replayResource(String scope,String actor,String key,long targetId,String requestHash){
        List<Map<String,Object>> rows=jdbc.queryForList("select request_hash,resource_id,response_json from idempotency_record where operation_scope=? and actor_id=? and idempotency_key=?",scope,actor,key);
        if(rows.isEmpty())return Optional.empty();
        Map<String,Object> row=rows.get(0);
        String response=String.valueOf(row.get("response_json"));
        long recordedTarget=response.startsWith("target:")?Long.parseLong(response.substring(7)):Long.parseLong(String.valueOf(row.get("resource_id")));
        if(recordedTarget!=targetId||!requestHash.equals(String.valueOf(row.get("request_hash"))))throw new R3ConflictException("Idempotency-Key 已绑定到其他请求");
        return Optional.of(Long.parseLong(String.valueOf(row.get("resource_id"))));
    }
    private void remember(String scope,String actor,String key,long targetId,String requestHash){remember(scope,actor,key,targetId,requestHash,targetId);}
    private void remember(String scope,String actor,String key,long targetId,String requestHash,long resultId){jdbc.update("insert into idempotency_record(operation_scope,actor_id,idempotency_key,request_hash,resource_id,response_json,created_at,expires_at) values(?,?,?,?,?,?,current_timestamp,?)",scope,actor,key,requestHash,String.valueOf(resultId),"target:"+targetId,LocalDateTime.now().plusDays(1));}
    private static String storedKey(String scope,String actor,String key,long targetId){return requestHash(scope+"|"+actor+"|"+targetId+"|"+key);}
    private static String requestHash(String value){
        try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    private static String normalize(String value){return value==null?"":value.trim();}
    private void appendAudit(String actor,String role,String action,String resource,long id,String details){audit.append(new AuditEventCommand(actor,role,action,resource,String.valueOf(id),"SUCCESS","业务操作",details,UUID.randomUUID().toString()));}

    /** 创建处方命令。 */
    public record PrescriptionCommand(long encounterId,String diagnosis,List<PrescriptionItemCommand> items){}
    /** 处方药品、数量和用法命令。 */
    public record PrescriptionItemCommand(long skuId,int quantity,String dosage){}
    /** 药师审方命令。 */
    public record ReviewCommand(boolean approved,String note){}
    /** 处方聚合视图。 */
    public record PrescriptionView(long id,long encounterId,long patientId,String diagnosis,String status,List<PrescriptionItemView> items){}
    /** 处方明细视图。 */
    public record PrescriptionItemView(long id,long skuId,String medicineName,int quantity,String dosage){}
    /** 创建账单命令。 */
    public record InvoiceCommand(long patientId,Long prescriptionId,List<InvoiceLineCommand> lines){}
    /** 收费项目命令。 */
    public record InvoiceLineCommand(String description,BigDecimal amount){}
    /** 支付命令。 */
    public record PaymentCommand(BigDecimal amount,String channel){}
    /** 退款命令。 */
    public record RefundCommand(BigDecimal amount,String reason){}
    /** 账单金额与状态视图。 */
    public record InvoiceView(long id,long patientId,Long prescriptionId,String status,BigDecimal totalAmount,BigDecimal paidAmount,BigDecimal refundedAmount){}
    /** 医保资格检查视图，包含模拟标识。 */
    public record EligibilityView(long patientId,boolean eligible,boolean simulation,String resultCode){}
    /** 医保申报命令。 */
    public record ClaimCommand(long invoiceId,BigDecimal amount){}
    /** 医保申报状态视图。 */
    public record ClaimView(long id,long invoiceId,String status,BigDecimal claimedAmount,BigDecimal settledAmount,boolean simulation,String externalReference){}
}
