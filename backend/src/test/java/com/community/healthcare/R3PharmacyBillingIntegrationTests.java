package com.community.healthcare;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import com.community.healthcare.pharmacy.infrastructure.R3ConflictException;
import com.community.healthcare.pharmacy.infrastructure.R3PharmacyBillingService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:r3-api;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.defer-datasource-initialization=true", "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:r5-test-references.sql,classpath:db/migration/V9__r3_pharmacy_inventory_billing.sql",
        "app.bootstrap.enabled=false", "app.demo-schema-migration.enabled=false"
})
@ActiveProfiles("demo")
class R3PharmacyBillingIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired R3PharmacyBillingService service;
    MockMvc mvc;

    @BeforeEach void setUp() { mvc = webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void signedEncounterToDispenseInvoicePaymentRefundAndSimulatedClaimIsAtomicAndOwned() throws Exception {
        Fixture f = fixture();
        long prescriptionId = number(staff(post("/api/v1/staff/pharmacy/prescriptions"), f.doctor, "DOCTOR", Map.of(
                "encounterId", f.encounter, "diagnosis", "原发性高血压", "items", List.of(Map.of(
                        "skuId", f.sku, "quantity", 12, "dosage", "每日一次"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn(), "$.id");
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/sign", prescriptionId)
                        .header("Idempotency-Key", "sign-" + f.suffix), f.doctor, "DOCTOR", Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/review", prescriptionId)
                        .header("Idempotency-Key", "review-" + f.suffix), f.pharmacist, "PHARMACIST",
                        Map.of("approved", true, "note", "用药适宜"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/pick", prescriptionId),
                f.pharmacist, "PHARMACIST", Map.of()).andExpect(status().isOk());
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/check", prescriptionId),
                f.pharmacist, "PHARMACIST", Map.of()).andExpect(status().isOk());
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/dispense", prescriptionId)
                        .header("Idempotency-Key", "dispense-" + f.suffix), f.pharmacist, "PHARMACIST", Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPENSED"));
        staff(post("/api/v1/staff/pharmacy/prescriptions/{id}/dispense", prescriptionId)
                        .header("Idempotency-Key", "dispense-" + f.suffix), f.pharmacist, "PHARMACIST", Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPENSED"));
        assertThat(jdbc.queryForObject("select quantity_on_hand from inventory_batch where id=?", Integer.class, f.batch)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from inventory_transaction where reference_id=?", Integer.class,
                String.valueOf(prescriptionId))).isEqualTo(1);

        long invoiceId = number(staff(post("/api/v1/staff/billing/invoices"), f.registrar, "REGISTRAR", Map.of(
                "patientId", f.patient, "prescriptionId", prescriptionId,
                "lines", List.of(Map.of("description", "药品费用", "amount", "36.00"))))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        staff(post("/api/v1/staff/billing/invoices/{id}/issue", invoiceId), f.registrar, "REGISTRAR", Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ISSUED"));
        staff(post("/api/v1/staff/billing/invoices/{id}/payments", invoiceId).header("Idempotency-Key", "pay-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "36.00", "channel", "CASH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        resident(get("/api/v1/resident/billing/invoices"), f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId));
        resident(get("/api/v1/resident/billing/invoices"), f.otherPatient).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        staff(post("/api/v1/staff/billing/insurance/claims").header("Idempotency-Key", "claim-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("invoiceId", invoiceId, "amount", "20.00"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        staff(post("/api/v1/staff/billing/invoices/{id}/refunds", invoiceId).header("Idempotency-Key", "refund-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "36.00", "reason", "测试退费"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUNDED"));
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action in ('PRESCRIPTION_REVIEWED','PRESCRIPTION_DISPENSED','BILLING_REFUNDED','INSURANCE_CLAIM_SUBMITTED')", Integer.class)).isGreaterThanOrEqualTo(4);
    }

    @Test
    void idempotencyKeyIsBoundToActorOperationTargetAndRequestPayload() throws Exception {
        Fixture f = fixture();
        long first = createIssuedInvoice(f, "10.00");
        long second = createIssuedInvoice(f, "10.00");

        staff(post("/api/v1/staff/billing/invoices/{id}/payments", first).header("Idempotency-Key", "bound-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "5.00", "channel", "CASH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paidAmount").value(5.0));
        staff(post("/api/v1/staff/billing/invoices/{id}/payments", first).header("Idempotency-Key", "bound-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "5.00", "channel", "CASH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paidAmount").value(5.0));
        staff(post("/api/v1/staff/billing/invoices/{id}/payments", second).header("Idempotency-Key", "bound-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "5.00", "channel", "CASH"))
                .andExpect(status().isConflict());
        staff(post("/api/v1/staff/billing/invoices/{id}/payments", first).header("Idempotency-Key", "bound-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "6.00", "channel", "CASH"))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentRefundsCannotExceedPaidAmount() throws Exception {
        Fixture f = fixture();
        long invoice = createIssuedInvoice(f, "100.00");
        staff(post("/api/v1/staff/billing/invoices/{id}/payments", invoice).header("Idempotency-Key", "pay-full-"+f.suffix),
                f.registrar, "REGISTRAR", Map.of("amount", "100.00", "channel", "CASH")).andExpect(status().isOk());
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> refundAfter(start, f.registrar, invoice, "refund-a-"+f.suffix, "registrar-a"));
            var b = executor.submit(() -> refundAfter(start, f.registrar, invoice, "refund-b-"+f.suffix, "registrar-b"));
            start.countDown();
            List<Object> results = List.of(result(a.get(10, TimeUnit.SECONDS)), result(b.get(10, TimeUnit.SECONDS)));
            assertThat(results.stream().filter(R3PharmacyBillingService.InvoiceView.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(R3ConflictException.class::isInstance)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select refunded_amount from billing_invoice where id=?", java.math.BigDecimal.class, invoice)).isEqualByComparingTo("80.00");
        assertThat(jdbc.queryForObject("select count(*) from billing_refund where invoice_id=?", Integer.class, invoice)).isEqualTo(1);
    }

    private Object refundAfter(CountDownLatch start,long registrar,long invoice,String key,String actor) {
        try {
            start.await();
            return service.refund(registrar, invoice, key,
                    new R3PharmacyBillingService.RefundCommand(new java.math.BigDecimal("80.00"), "并发退款"), actor);
        } catch (Exception ex) { return ex; }
    }
    private Object result(Object value) { return value; }
    private long createIssuedInvoice(Fixture f,String amount) throws Exception {
        long id=number(staff(post("/api/v1/staff/billing/invoices"),f.registrar,"REGISTRAR",Map.of(
                "patientId",f.patient,"lines",List.of(Map.of("description","测试费用","amount",amount))))
                .andExpect(status().isCreated()).andReturn(),"$.id");
        staff(post("/api/v1/staff/billing/invoices/{id}/issue",id),f.registrar,"REGISTRAR",Map.of()).andExpect(status().isOk());
        return id;
    }

    private Fixture fixture() {
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jdbc.update("insert into organization(code,name,active,created_at,updated_at,version) values(?,?,true,current_timestamp,current_timestamp,0)", "R3O"+s, "中心");
        long org = jdbc.queryForObject("select id from organization where code=?", Long.class, "R3O"+s);
        jdbc.update("insert into site(organization_id,code,name,site_type,active,created_at,updated_at,version) values(?,?,?,'CENTER',true,current_timestamp,current_timestamp,0)", org,"R3S"+s,"中心站");
        long site = jdbc.queryForObject("select id from site where code=?", Long.class,"R3S"+s);
        long doctor=staff(org,"D"+s,"DOCTOR"), pharmacist=staff(org,"P"+s,"PHARMACIST"), registrar=staff(org,"R"+s,"REGISTRAR");
        long patient=patient("R3P"+s), other=patient("R3Q"+s);
        jdbc.update("insert into clinical_encounter(appointment_id,patient_id,staff_profile_id,status,started_at,signed_at,created_at,updated_at,version) values(?,?,?,?,current_timestamp,current_timestamp,current_timestamp,current_timestamp,1)", 900000000L+patient,patient,doctor,"SIGNED");
        long encounter=jdbc.queryForObject("select id from clinical_encounter where patient_id=? and staff_profile_id=? order by id desc limit 1",Long.class,patient,doctor);
        jdbc.update("insert into rx_medicine_sku(code,name,strength,unit,active,created_at,updated_at,version) values(?,?,?,?,true,current_timestamp,current_timestamp,0)","SKU"+s,"苯磺酸氨氯地平","5mg","片");
        long sku=jdbc.queryForObject("select id from rx_medicine_sku where code=?",Long.class,"SKU"+s);
        jdbc.update("insert into inventory_warehouse(site_id,code,name,active,created_at,updated_at,version) values(?,?,?,true,current_timestamp,current_timestamp,0)",site,"WH"+s,"药房");
        long wh=jdbc.queryForObject("select id from inventory_warehouse where code=?",Long.class,"WH"+s);
        jdbc.update("insert into inventory_batch(warehouse_id,sku_id,lot_number,expires_on,quantity_on_hand,created_at,updated_at,version) values(?,?,?,?,20,current_timestamp,current_timestamp,0)",wh,sku,"LOT"+s,java.sql.Date.valueOf(java.time.LocalDate.now().plusYears(1)));
        long batch=jdbc.queryForObject("select id from inventory_batch where lot_number=?",Long.class,"LOT"+s);
        return new Fixture(s,doctor,pharmacist,registrar,patient,other,encounter,sku,batch);
    }
    private long staff(long org,String no,String type){ jdbc.update("insert into staff_profile(organization_id,staff_no,name,staff_type,account_status,active,created_at,updated_at,version) values(?,?,?,?,'ACTIVE',true,current_timestamp,current_timestamp,0)",org,no,no,type); return jdbc.queryForObject("select id from staff_profile where staff_no=?",Long.class,no); }
    private long patient(String id){ jdbc.update("insert into patient(id_card,name,gender,birth_date,balance,active,created_at,updated_at,version) values(?,?,'其他','1990-01-01',0,true,current_timestamp,current_timestamp,0)",id,id); return jdbc.queryForObject("select id from patient where id_card=?",Long.class,id); }
    private org.springframework.test.web.servlet.ResultActions staff(MockHttpServletRequestBuilder req,long id,String role,Object body)throws Exception{return mvc.perform(req.with(jwt().jwt(b->b.subject(role+id).claim("roles",List.of(role)).claim("role",role).claim("staffId",id).claim("staffProfileId",id).claim("mustChangePassword",false)).authorities(new SimpleGrantedAuthority("ROLE_"+role))).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));}
    private org.springframework.test.web.servlet.ResultActions resident(MockHttpServletRequestBuilder req,long id)throws Exception{return mvc.perform(req.with(jwt().jwt(b->b.subject("resident"+id).claim("roles",List.of("RESIDENT")).claim("role","RESIDENT").claim("patientId",id).claim("mustChangePassword",false)).authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))));}
    private long number(MvcResult r,String p)throws Exception{return ((Number)JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8),p)).longValue();}
    private record Fixture(String suffix,long doctor,long pharmacist,long registrar,long patient,long otherPatient,long encounter,long sku,long batch){}
}
