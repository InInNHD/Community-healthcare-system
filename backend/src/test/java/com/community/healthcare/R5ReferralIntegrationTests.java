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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:r5-api;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.defer-datasource-initialization=true", "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:r5-test-references.sql,classpath:db/migration/V11__r5_referral_integration_notification_quality.sql",
        "app.bootstrap.enabled=false", "app.demo-schema-migration.enabled=false"
})
@ActiveProfiles("demo")
class R5ReferralIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.community.healthcare.referral.application.R5PlatformService referrals;
    MockMvc mvc;

    @BeforeEach void setUp() { mvc = webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void referralOutboxRecordReleaseMessagingFeedbackAndQualityFormAnOwnedClosedLoop() throws Exception {
        Fixture f = fixture();
        long referralId = number(staff(post("/api/v1/staff/referrals").header("Idempotency-Key", "ref-" + f.suffix),
                f.doctor, "DOCTOR", Map.of("patientId", f.patient, "targetOrganization", "市人民医院",
                        "targetDepartment", "呼吸科", "reason", "COPD进一步评估"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn(), "$.id");

        resident(post("/api/v1/resident/referrals/{id}/consent", referralId), f.patient, Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONSENTED"));
        staff(post("/api/v1/staff/referrals/{id}/submit", referralId).header("Idempotency-Key", "submit-" + f.suffix),
                f.doctor, "DOCTOR", Map.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id=?", Integer.class,
                String.valueOf(referralId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action='REFERRAL_SUBMITTED' and resource_id=?",
                Integer.class, String.valueOf(referralId))).isEqualTo(1);

        long pendingOutboxId = outboxId(referralId);
        assertThat(referrals.dueOutboxIds(20)).contains(pendingOutboxId);
        assertThat(referrals.dispatchDueOutbox(pendingOutboxId)).isTrue();
        assertThat(referrals.dispatchDueOutbox(pendingOutboxId)).isFalse();
        admin(post("/api/v1/admin/integrations/outbox/{id}/retry", pendingOutboxId), Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.simulation").value(true));
        for (String next : List.of("ACCEPTED", "SCHEDULED", "ATTENDED")) {
            staff(post("/api/v1/staff/referrals/{id}/receipts", referralId), f.doctor, "DOCTOR",
                    Map.of("status", next, "note", "模拟区域平台回执"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        resident(post("/api/v1/resident/referrals/{id}/feedback", referralId), f.patient,
                Map.of("rating", 5, "comments", "转诊顺利"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("FEEDBACK_RECEIVED"));
        for (String next : List.of("CONTINUE_MANAGEMENT", "CLOSED")) {
            staff(post("/api/v1/staff/referrals/{id}/receipts", referralId), f.doctor, "DOCTOR",
                    Map.of("status", next, "note", "回到社区继续管理"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }

        resident(get("/api/v1/resident/referrals/{id}", referralId), f.patient, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value(f.patient));
        resident(get("/api/v1/resident/referrals/{id}", referralId), f.otherPatient, null)
                .andExpect(status().isForbidden());

        resident(post("/api/v1/resident/records/releases"), f.patient,
                Map.of("referralId", referralId, "scopeCode", "REFERRAL_SUMMARY", "purpose", "转诊资料交换"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED"));
        resident(post("/api/v1/resident/messages"), f.patient,
                Map.of("subject", "复诊时间咨询", "body", "请问何时可以复诊？"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.diagnostic").value(false));
        resident(get("/api/v1/resident/messages"), f.patient, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].patientId").value(f.patient));

        resident(post("/api/v1/resident/feedback"), f.patient,
                Map.of("businessType", "REFERRAL", "businessId", String.valueOf(referralId), "rating", 5,
                        "comments", "服务满意"))
                .andExpect(status().isCreated());
        resident(post("/api/v1/resident/feedback"), f.patient,
                Map.of("businessType", "REFERRAL", "businessId", String.valueOf(referralId), "rating", 5,
                        "comments", "重复评价"))
                .andExpect(status().isConflict());
        admin(post("/api/v1/admin/quality/snapshots/refresh"), Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.referralCount").isNumber())
                .andExpect(jsonPath("$.averageRating").isNumber());
    }

    @Test
    void concurrentReferralSubmissionCreatesOnlyOneTransitionAndOutboxEvent() throws Exception {
        Fixture f=fixture();
        var created=referrals.create(f.doctor,"concurrent-create-"+f.suffix,
                new com.community.healthcare.referral.application.R5PlatformService.CreateReferral(
                        f.patient,null,"市医院","全科","并发检查"));
        referrals.consent(f.patient,created.id());
        CountDownLatch start=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->{start.await();return submitResult(f.doctor,created.id(),"event-a-"+f.suffix);});
            var b=pool.submit(()->{start.await();return submitResult(f.doctor,created.id(),"event-b-"+f.suffix);});
            start.countDown();
            a.get(); b.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id=?",Integer.class,String.valueOf(created.id()))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from referral_history where referral_id=? and to_status='SUBMITTED'",Integer.class,created.id())).isEqualTo(1);
    }

    @Test
    void referralCreationRejectsWrongJobAndOutOfScopePatient() throws Exception {
        Fixture f=fixture();
        staff(post("/api/v1/staff/referrals").header("Idempotency-Key","wrong-role-"+f.suffix),f.doctor,"NURSE",
                Map.of("patientId",f.patient,"targetOrganization","市医院","targetDepartment","全科","reason","测试"))
                .andExpect(status().isForbidden());
        staff(post("/api/v1/staff/referrals").header("Idempotency-Key","wrong-scope-"+f.suffix),f.doctor,"DOCTOR",
                Map.of("patientId",f.otherPatient,"targetOrganization","市医院","targetDepartment","全科","reason","测试"))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedOutboxDeliveryBacksOffAndCreatesOnlyOneDeadLetter() {
        String key = "failing-" + UUID.randomUUID();
        jdbc.update("insert into outbox_event(event_key,aggregate_type,aggregate_id,event_type,payload_json,status,attempts,created_at) " +
                "values(?,'TEST',?,'TEST_EVENT','{\"simulateFailure\":true}','PENDING',0,current_timestamp)", key, key);
        long id = jdbc.queryForObject("select id from outbox_event where event_key=?", Long.class, key);

        assertThat(referrals.retryOutbox(id,"admin").status()).isEqualTo("FAILED");
        assertThat(referrals.retryOutbox(id,"admin").status()).isEqualTo("FAILED");
        assertThat(referrals.retryOutbox(id,"admin").status()).isEqualTo("DEAD");
        assertThat(referrals.retryOutbox(id,"admin").status()).isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("select count(*) from integration_dead_letter where outbox_event_id=?",
                Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select next_attempt_at>current_timestamp from outbox_event where id=?",
                Boolean.class,id)).isTrue();
    }

    private boolean submitResult(long staff,long id,String key){try{referrals.submit(staff,id,key);return true;}catch(org.springframework.dao.OptimisticLockingFailureException ex){return false;}}

    private Fixture fixture() {
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jdbc.update("insert into organization(code,name,active,created_at,updated_at,version) values(?,?,true,current_timestamp,current_timestamp,0)", "R5O"+s, "中心");
        long org = jdbc.queryForObject("select id from organization where code=?", Long.class, "R5O"+s);
        jdbc.update("insert into site(organization_id,code,name,site_type,active,created_at,updated_at,version) values(?,?,?,'CENTER',true,current_timestamp,current_timestamp,0)",org,"R5S"+s,"中心站");
        long site=jdbc.queryForObject("select id from site where code=?",Long.class,"R5S"+s);
        jdbc.update("insert into staff_profile(organization_id,staff_no,name,staff_type,account_status,active,created_at,updated_at,version) values(?,?,?,'DOCTOR','ACTIVE',true,current_timestamp,current_timestamp,0)", org,"R5D"+s,"医生");
        long doctor = jdbc.queryForObject("select id from staff_profile where staff_no=?", Long.class, "R5D"+s);
        long patient=patient("R5P"+s), other=patient("R5Q"+s);
        jdbc.update("insert into staff_site_assignment(staff_profile_id,site_id,role_code,valid_from,active,created_at) values(?,?,'DOCTOR',current_timestamp,true,current_timestamp)",doctor,site);
        jdbc.update("insert into patient_site_enrollment(patient_id,site_id,enrolled_at,active,created_at) values(?,?,current_timestamp,true,current_timestamp)",patient,site);
        return new Fixture(s, doctor, patient, other);
    }
    private long patient(String id){ jdbc.update("insert into patient(id_card,name,gender,birth_date,balance,active,created_at,updated_at,version) values(?,?,'其他','1990-01-01',0,true,current_timestamp,current_timestamp,0)",id,id); return jdbc.queryForObject("select id from patient where id_card=?",Long.class,id); }
    private long outboxId(long referral){return jdbc.queryForObject("select id from outbox_event where aggregate_id=?",Long.class,String.valueOf(referral));}
    private org.springframework.test.web.servlet.ResultActions staff(MockHttpServletRequestBuilder req,long id,String role,Object body)throws Exception{return mvc.perform(req.with(jwt().jwt(b->b.subject(role+id).claim("roles",List.of(role)).claim("role",role).claim("staffId",id).claim("staffProfileId",id).claim("mustChangePassword",false)).authorities(new SimpleGrantedAuthority("ROLE_"+role))).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));}
    private org.springframework.test.web.servlet.ResultActions resident(MockHttpServletRequestBuilder req,long id,Object body)throws Exception{var b=req.with(jwt().jwt(j->j.subject("resident"+id).claim("roles",List.of("RESIDENT")).claim("role","RESIDENT").claim("patientId",id).claim("mustChangePassword",false)).authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))); if(body!=null)b.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)); return mvc.perform(b);}
    private org.springframework.test.web.servlet.ResultActions admin(MockHttpServletRequestBuilder req,Object body)throws Exception{return mvc.perform(req.with(jwt().jwt(j->j.subject("admin").claim("roles",List.of("ADMIN")).claim("role","ADMIN").claim("mustChangePassword",false)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));}
    private long number(MvcResult r,String p)throws Exception{return ((Number) JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8),p)).longValue();}
    private record Fixture(String suffix,long doctor,long patient,long otherPatient){}
}
