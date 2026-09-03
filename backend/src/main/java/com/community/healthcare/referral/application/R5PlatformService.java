package com.community.healthcare.referral.application;

import com.community.healthcare.integration.ExchangeReceipt;
import com.community.healthcare.integration.RegionalHealthPlatformPort;
import com.community.healthcare.observability.RequestCorrelationFilter;
import com.community.healthcare.referral.domain.ReferralCase;
import com.community.healthcare.referral.domain.ReferralStatus;
import com.community.healthcare.residentregistry.application.StaffPatientScope;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * R5 双向转诊、区域平台交换、居民互动与质量快照的应用服务。
 *
 * <p>转诊提交采用本地事务出站表，使业务状态和待发送事件原子落库；外部调用失败时
 * 保留事件供重试与对账，避免交换消息丢失。</p>
 */
@Service
public class R5PlatformService {
    private final JdbcTemplate jdbc;
    private final RegionalHealthPlatformPort platform;
    private final ObjectMapper json;
    private final StaffPatientScope scope;

    public R5PlatformService(JdbcTemplate jdbc, RegionalHealthPlatformPort platform, ObjectMapper json,StaffPatientScope scope) {
        this.jdbc = jdbc; this.platform = platform; this.json = json;this.scope=scope;
    }

    /** 在医护数据范围内幂等创建转诊草稿。 */
    @Transactional
    public ReferralView create(long staffId, String key, CreateReferral command) {
        scope.require(staffId,command.patientId());
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key 不能为空");
        List<ReferralView> replay = jdbc.query("select * from referral_case where idempotency_key=?", this::referral, key);
        if (!replay.isEmpty()) {
            requireStaff(replay.get(0), staffId);
            return replay.get(0);
        }
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("insert into referral_case(patient_id,created_by_staff_id,encounter_id,target_organization,target_department,reason,status,idempotency_key,created_at,updated_at,version) values(?,?,?,?,?,?,'DRAFT',?,current_timestamp,current_timestamp,0)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, command.patientId()); ps.setLong(2, staffId);
            if (command.encounterId() == null) ps.setNull(3, java.sql.Types.BIGINT); else ps.setLong(3, command.encounterId());
            ps.setString(4, required(command.targetOrganization(), "目标机构"));
            ps.setString(5, required(command.targetDepartment(), "目标科室"));
            ps.setString(6, required(command.reason(), "转诊原因")); ps.setString(7, key); return ps;
        }, holder);
        long id = holder.getKey().longValue();
        history(id, null, ReferralStatus.DRAFT, "STAFF", staffId, "创建转诊草稿");
        audit("staff:"+staffId, "DOCTOR", "REFERRAL_CREATED", "REFERRAL", id, "SUCCESS", command.reason());
        return getForStaff(id, staffId);
    }

    /** 由居民本人确认转诊授权。 */
    @Transactional
    public ReferralView consent(long patientId, long id) {
        ReferralView current = getOwned(id, patientId); ReferralCase aggregate = aggregate(current);
        aggregate.consent(patientId); updateStatus(current, aggregate.status());
        jdbc.update("update referral_case set consented_at=current_timestamp where id=?", id);
        history(id, current.status(), aggregate.status(), "RESIDENT", patientId, "居民同意转诊");
        audit("patient:"+patientId, "RESIDENT", "REFERRAL_CONSENTED", "REFERRAL", id, "SUCCESS", "居民知情同意");
        return getOwned(id, patientId);
    }

    /** 将已获居民同意的转诊提交，并在同一事务中写入出站事件。 */
    @Transactional
    public ReferralView submit(long staffId, long id, String eventKey) {
        ReferralView current = getForStaff(id, staffId);
        if (current.status() == ReferralStatus.SUBMITTED) return current;
        ReferralCase aggregate = aggregate(current); aggregate.transitionTo(ReferralStatus.SUBMITTED);
        updateStatus(current, aggregate.status());
        jdbc.update("update referral_case set submitted_at=current_timestamp where id=?", id);
        history(id, current.status(), aggregate.status(), "STAFF", staffId, "提交区域平台");
        String payload = write(Map.of("referralId", id, "patientId", current.patientId(), "targetOrganization", current.targetOrganization()));
        jdbc.update("insert into outbox_event(event_key,aggregate_type,aggregate_id,event_type,payload_json,status,attempts,created_at) values(?,?,?,?,?,'PENDING',0,current_timestamp)",
                required(eventKey, "Idempotency-Key"), "REFERRAL", String.valueOf(id), "REFERRAL_SUBMITTED", payload);
        audit("staff:"+staffId, "DOCTOR", "REFERRAL_SUBMITTED", "REFERRAL", id, "SUCCESS", "进入可靠交换队列");
        return getForStaff(id, staffId);
    }

    /** 登记上级机构回执并推进转诊状态。 */
    @Transactional
    public ReferralView receipt(long staffId, long id, ReferralStatus target, String note) {
        ReferralView current = getForStaff(id, staffId); ReferralCase aggregate = aggregate(current);
        aggregate.transitionTo(target); updateStatus(current, target);
        if (target == ReferralStatus.CLOSED) jdbc.update("update referral_case set closed_at=current_timestamp where id=?", id);
        history(id, current.status(), target, "STAFF", staffId, note);
        audit("staff:"+staffId, "DOCTOR", "REFERRAL_STATUS_CHANGED", "REFERRAL", id, "SUCCESS", target.name());
        return getForStaff(id, staffId);
    }

    /** 保存居民对本人转诊服务的评分和意见。 */
    @Transactional
    public ReferralView referralFeedback(long patientId, long id, int rating, String comments) {
        checkRating(rating); ReferralView current = getOwned(id, patientId); ReferralCase aggregate = aggregate(current);
        aggregate.transitionTo(ReferralStatus.FEEDBACK_RECEIVED);
        jdbc.update("insert into referral_feedback(referral_id,patient_id,rating,comments,created_at) values(?,?,?,?,current_timestamp)", id, patientId, rating, comments);
        updateStatus(current, aggregate.status()); history(id, current.status(), aggregate.status(), "RESIDENT", patientId, "居民反馈");
        audit("patient:"+patientId, "RESIDENT", "REFERRAL_FEEDBACK_CREATED", "REFERRAL", id, "SUCCESS", "rating="+rating);
        return getOwned(id, patientId);
    }

    /** 按居民所有权查询本人转诊。 */
    public ReferralView getOwned(long id, long patientId) {
        ReferralView view = get(id); if (view.patientId()!=patientId) throw new AccessDeniedException("无权访问他人的转诊记录"); return view;
    }
    /** 在医护数据范围内查询，并限制为转诊创建者。 */
    public ReferralView getForStaff(long id, long staffId) {
        ReferralView view = get(id); requireStaff(view, staffId); return view;
    }
    public List<ReferralView> residentReferrals(long patientId) { return jdbc.query("select * from referral_case where patient_id=? order by id desc", this::referral, patientId); }
    /** 返回当前医生创建的转诊单。 */
    public List<ReferralView> staffReferrals(long staffId) { return jdbc.query("select * from referral_case where created_by_staff_id=? order by id desc", this::referral, staffId); }

    /**
     * 重试出站事件并保存每次交换结果；失败状态和次数同样提交，供退避重试和人工对账。
     */
    @Transactional
    public WorkbenchView retryOutbox(long id, String actor) {
        Map<String,Object> row;
        try { row = jdbc.queryForMap("select * from outbox_event where id=?", id); }
        catch (EmptyResultDataAccessException ex) { throw new jakarta.persistence.EntityNotFoundException("交换事件不存在"); }
        if ("SENT".equals(row.get("status"))) {
            return new WorkbenchView(id,"SENT",((Number)row.get("attempts")).intValue(),null,true);
        }
        String key=(String)row.get("event_key"), payload=(String)row.get("payload_json"); int attempts=((Number)row.get("attempts")).intValue()+1;
        try {
            ExchangeReceipt receipt=platform.submit(key,payload);
            long exchangeId=insertExchange(id,payload,write(receipt),"SENT",receipt.externalReference());
            jdbc.update("update outbox_event set status='SENT',attempts=?,processed_at=current_timestamp,last_error=null,next_attempt_at=null where id=?",attempts,id);
            if ("REFERRAL".equals(row.get("aggregate_type"))) jdbc.update("insert into referral_exchange_link(referral_id,integration_exchange_id,created_at) values(?,?,current_timestamp)",Long.parseLong((String)row.get("aggregate_id")),exchangeId);
            audit(actor,auditRole(actor),"INTEGRATION_RETRIED","OUTBOX_EVENT",id,"SUCCESS","模拟适配器");
            return new WorkbenchView(id,"SENT",attempts,receipt.externalReference(),receipt.simulation());
        } catch (RuntimeException ex) {
            insertExchange(id,payload,null,"FAILED",null);
            String status=attempts>=3?"DEAD":"FAILED";
            LocalDateTime nextAttempt = LocalDateTime.now().plusSeconds(Math.min(900, 30L << Math.min(attempts - 1, 5)));
            jdbc.update("update outbox_event set status=?,attempts=?,last_error=?,next_attempt_at=? where id=?",status,attempts,trim(ex.getMessage(),1000),nextAttempt,id);
            if (attempts>=3) {
                int existing = jdbc.queryForObject("select count(*) from integration_dead_letter where outbox_event_id=?",Integer.class,id);
                if (existing == 0) jdbc.update("insert into integration_dead_letter(outbox_event_id,payload_json,failure_reason,attempts,failed_at) values(?,?,?,?,current_timestamp)",id,payload,trim(ex.getMessage(),1000),attempts);
                else jdbc.update("update integration_dead_letter set failure_reason=?,attempts=?,failed_at=current_timestamp,resolved_at=null,resolved_by=null where outbox_event_id=?",trim(ex.getMessage(),1000),attempts,id);
            }
            audit(actor,auditRole(actor),"INTEGRATION_RETRIED","OUTBOX_EVENT",id,"FAILED",ex.getMessage());
            return new WorkbenchView(id,status,attempts,null,true);
        }
    }

    /** 查询到期的待交换事件；包含超时的处理中事件，以便进程异常退出后自动恢复。 */
    public List<Long> dueOutboxIds(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("select id from outbox_event where status in ('PENDING','FAILED','PROCESSING') " +
                "and (next_attempt_at is null or next_attempt_at<=current_timestamp) order by created_at limit ?",Long.class,safeLimit);
    }

    /** 原子认领并发送一个到期事件；其他实例认领成功时本实例直接跳过。 */
    @Transactional
    public boolean dispatchDueOutbox(long id) {
        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(5);
        int claimed = jdbc.update("update outbox_event set status='PROCESSING',next_attempt_at=? where id=? " +
                "and status in ('PENDING','FAILED','PROCESSING') and (next_attempt_at is null or next_attempt_at<=current_timestamp)",
                leaseUntil,id);
        if (claimed == 0) return false;
        retryOutbox(id,"system:outbox-dispatcher");
        return true;
    }

    /** 查询区域平台交换工作台数据。 */
    public List<Map<String,Object>> outbox() { return jdbc.queryForList("select id,event_key,event_type,status,attempts,last_error,created_at from outbox_event order by id desc"); }

    /** 由居民创建限定资料范围和使用目的的授权申请。 */
    @Transactional
    public Map<String,Object> release(long patientId, Long referralId, String scope, String purpose) {
        if (referralId!=null) getOwned(referralId,patientId);
        KeyHolder h=insert("insert into record_release(patient_id,referral_id,scope_code,purpose,status,requested_at) values(?,?,?,?,'REQUESTED',current_timestamp)", patientId,referralId,required(scope,"档案范围"),required(purpose,"用途"));
        long id=h.getKey().longValue(); audit("patient:"+patientId,"RESIDENT","RECORD_RELEASE_REQUESTED","RECORD_RELEASE",id,"SUCCESS",purpose);
        return Map.of("id",id,"patientId",patientId,"status","REQUESTED","scopeCode",scope);
    }

    /** 创建非诊断性的居民健康咨询留言。 */
    @Transactional
    public Map<String,Object> message(long patientId, String subject, String body) {
        KeyHolder h=insert("insert into notification_message(patient_id,direction,category,subject,body,diagnostic,status,created_at) values(?,'INBOUND','HEALTH_CONSULTATION',?,?,false,'CREATED',current_timestamp)",patientId,required(subject,"主题"),required(body,"留言"));
        long id=h.getKey().longValue(); jdbc.update("insert into notification_delivery(message_id,channel,status,attempts) values(?,'PORTAL','DELIVERED',1)",id);
        audit("patient:"+patientId,"RESIDENT","NON_DIAGNOSTIC_MESSAGE_CREATED","MESSAGE",id,"SUCCESS","不用于诊断或处方");
        return Map.of("id",id,"patientId",patientId,"diagnostic",false,"status","CREATED");
    }
    /** 查询居民本人的咨询留言及回复。 */
    public List<Map<String,Object>> messages(long patientId) {
        return jdbc.query("select id,patient_id,direction,category,subject,body,diagnostic,status,created_at "
                        + "from notification_message where patient_id=? order by id desc",
                (rs, row) -> {
                    Map<String, Object> message = new java.util.LinkedHashMap<>();
                    message.put("id", rs.getLong("id"));
                    message.put("patientId", rs.getLong("patient_id"));
                    message.put("direction", rs.getString("direction"));
                    message.put("category", rs.getString("category"));
                    message.put("subject", rs.getString("subject"));
                    message.put("body", rs.getString("body"));
                    message.put("diagnostic", rs.getBoolean("diagnostic"));
                    message.put("status", rs.getString("status"));
                    message.put("createdAt", rs.getTimestamp("created_at").toInstant());
                    return message;
                }, patientId);
    }

    /** 返回当前工作人员服务范围内的居民留言。 */
    public List<Map<String,Object>> staffMessages(long staffId) {
        return jdbc.queryForList("select m.id,m.patient_id,p.name as patient_name,m.direction,m.category,m.subject,m.body,m.status,m.created_at "
                + "from notification_message m join patient p on p.id=m.patient_id where exists(select 1 from patient_site_enrollment pe "
                + "join staff_site_assignment sa on sa.site_id=pe.site_id where pe.patient_id=m.patient_id and pe.active=true "
                + "and sa.staff_profile_id=? and sa.active=true and sa.valid_from<=current_timestamp "
                + "and (sa.valid_to is null or sa.valid_to>current_timestamp)) order by m.id desc limit 100", staffId);
    }

    /** 对服务范围内的居民留言进行非诊断性答复。 */
    @Transactional
    public Map<String,Object> replyMessage(long staffId,long messageId,String body) {
        Map<String,Object> original;
        try { original=jdbc.queryForMap("select patient_id,subject from notification_message where id=?",messageId); }
        catch (EmptyResultDataAccessException ex) { throw new jakarta.persistence.EntityNotFoundException("留言不存在"); }
        long patientId=((Number)original.get("patient_id")).longValue();
        scope.require(staffId,patientId);
        KeyHolder holder=insert("insert into notification_message(patient_id,direction,category,subject,body,diagnostic,status,created_at) values(?,'OUTBOUND','HEALTH_REPLY',?,?,false,'CREATED',current_timestamp)",patientId,String.valueOf(original.get("subject")),required(body,"答复内容"));
        long id=holder.getKey().longValue();
        jdbc.update("insert into notification_delivery(message_id,channel,status,attempts) values(?,'PORTAL','DELIVERED',1)",id);
        jdbc.update("update notification_message set status='REPLIED' where id=?",messageId);
        audit("staff:"+staffId,"STAFF","MESSAGE_REPLIED","MESSAGE",id,"SUCCESS","非诊断性答复");
        return jdbc.queryForMap("select id,patient_id,direction,category,subject,body,status,created_at from notification_message where id=?",id);
    }

    /** 查询居民本人的档案开放申请。 */
    public List<Map<String,Object>> releases(long patientId){return jdbc.queryForList("select id,referral_id,scope_code,purpose,status,requested_at,expires_at,released_at,revoked_at from record_release where patient_id=? order by id desc",patientId);}

    /** 查询居民本人已提交的服务评价。 */
    public List<Map<String,Object>> feedback(long patientId){return jdbc.queryForList("select id,business_type,business_id,rating,comments,created_at from service_feedback where patient_id=? order by id desc",patientId);}

    /** 保存居民对指定业务服务的通用反馈。 */
    @Transactional
    public Map<String,Object> serviceFeedback(long patientId,String type,String businessId,int rating,String comments) {
        checkRating(rating); KeyHolder h=insert("insert into service_feedback(patient_id,business_type,business_id,rating,comments,created_at) values(?,?,?,?,?,current_timestamp)",patientId,required(type,"业务类型"),required(businessId,"业务ID"),rating,comments);
        long id=h.getKey().longValue(); audit("patient:"+patientId,"RESIDENT","SERVICE_FEEDBACK_CREATED","SERVICE_FEEDBACK",id,"SUCCESS",type+":"+businessId);
        return Map.of("id",id,"patientId",patientId,"rating",rating);
    }

    /** 汇总当月转诊量和平均满意度并更新质量快照。 */
    @Transactional
    public QualityView refreshQuality() {
        long count=jdbc.queryForObject("select count(*) from referral_case",Long.class);
        BigDecimal avg=jdbc.queryForObject("select coalesce(avg(rating),0) from service_feedback",BigDecimal.class);
        String period= LocalDate.now().toString().substring(0,7);
        upsertMetric(period,"REFERRAL_COUNT",BigDecimal.valueOf(count)); upsertMetric(period,"SERVICE_AVERAGE_RATING",avg);
        audit("admin","ADMIN","QUALITY_SNAPSHOT_REFRESHED","QUALITY_SNAPSHOT",period,"SUCCESS","aggregated");
        return new QualityView(period,count,avg);
    }
    /** 查询按期间保存的质量指标快照。 */
    public List<Map<String,Object>> qualitySnapshots(){return jdbc.queryForList("select period_key as periodKey,metric_code as metricCode,metric_value as metricValue,generated_at as generatedAt from quality_snapshot order by period_key desc,metric_code");}

    private ReferralView get(long id){try{return jdbc.queryForObject("select * from referral_case where id=?",this::referral,id);}catch(EmptyResultDataAccessException ex){throw new jakarta.persistence.EntityNotFoundException("转诊记录不存在");}}
    private ReferralView referral(java.sql.ResultSet rs,int n)throws java.sql.SQLException{return new ReferralView(rs.getLong("id"),rs.getLong("patient_id"),rs.getLong("created_by_staff_id"),ReferralStatus.valueOf(rs.getString("status")),rs.getString("target_organization"),rs.getString("target_department"),rs.getString("reason"));}
    private ReferralCase aggregate(ReferralView v){return ReferralCase.restore(v.patientId(),v.createdByStaffId(),v.status());}
    private void requireStaff(ReferralView v,long staff){scope.require(staff,v.patientId());if(v.createdByStaffId()!=staff)throw new AccessDeniedException("仅创建该转诊的医护可操作");}
    private void updateStatus(ReferralView current,ReferralStatus target){
        int affected=jdbc.update("update referral_case set status=?,updated_at=current_timestamp,version=version+1 where id=? and status=?",target.name(),current.id(),current.status().name());
        if(affected!=1)throw new OptimisticLockingFailureException("转诊状态已被其他操作修改，请刷新后重试");
    }
    private void history(long id,ReferralStatus from,ReferralStatus to,String actorType,long actor,String note){jdbc.update("insert into referral_history(referral_id,from_status,to_status,actor_type,actor_id,note,occurred_at) values(?,?,?,?,?,?,current_timestamp)",id,from==null?null:from.name(),to.name(),actorType,actor,trim(note,1000));}
    private void audit(String actor,String role,String action,String type,Object id,String outcome,String details){jdbc.update("insert into audit_event(occurred_at,actor,actor_role,action,resource_type,resource_id,outcome,purpose,details_json,correlation_id) values(current_timestamp,?,?,?,?,?,?,?,?,?)",actor,role,action,type,String.valueOf(id),outcome,"业务办理",trim(details,2000), RequestCorrelationFilter.current());}
    private String auditRole(String actor){return actor != null && actor.startsWith("system:") ? "SYSTEM" : "ADMIN";}
    private long insertExchange(long outbox,String request,String response,String status,String ref){KeyHolder h=insert("insert into integration_exchange(outbox_event_id,adapter_code,request_json,response_json,status,external_reference,attempted_at) values(?,'REGIONAL_MOCK',?,?,?,?,current_timestamp)",outbox,request,response,status,ref);return h.getKey().longValue();}
    private KeyHolder insert(String sql,Object...args){KeyHolder h=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);return ps;},h);return h;}
    private void upsertMetric(String p,String c,BigDecimal v){int n=jdbc.update("update quality_snapshot set metric_value=?,generated_at=current_timestamp where period_key=? and metric_code=?",v,p,c);if(n==0)jdbc.update("insert into quality_snapshot(period_key,metric_code,metric_value,generated_at) values(?,?,?,current_timestamp)",p,c,v);}
    private String write(Object v){try{return json.writeValueAsString(v);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static String required(String v,String name){if(v==null||v.isBlank())throw new IllegalArgumentException(name+"不能为空");return v.trim();}
    private static String trim(String v,int max){if(v==null)return null;return v.length()>max?v.substring(0,max):v;}
    private static void checkRating(int v){if(v<1||v>5)throw new IllegalArgumentException("评分必须为1到5");}

    /** 创建转诊命令。 */
    public record CreateReferral(long patientId,Long encounterId,String targetOrganization,String targetDepartment,String reason){}
    /** 转诊核心信息视图。 */
    public record ReferralView(long id,long patientId,long createdByStaffId,ReferralStatus status,String targetOrganization,String targetDepartment,String reason){}
    /** 出站事件重试结果视图。 */
    public record WorkbenchView(long id,String status,int attempts,String externalReference,boolean simulation){}
    /** 当前质量汇总结果。 */
    public record QualityView(String periodKey,long referralCount,BigDecimal averageRating){}
}
