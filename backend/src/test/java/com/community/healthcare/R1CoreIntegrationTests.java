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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:r1-core;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=0")
@ActiveProfiles("demo")
class R1CoreIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminManagesValidHierarchyProtectsIdentifiersAndAppendsAuditEvents() throws Exception {
        int auditBefore = jdbc.queryForObject("select count(*) from audit_event", Integer.class);
        String suffix = suffix();
        long firstOrganization = number(adminJson(post("/api/v1/admin/organizations"), Map.of(
                "code", "ORG-A-" + suffix, "name", "东城中心"))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        long secondOrganization = number(adminJson(post("/api/v1/admin/organizations"), Map.of(
                "code", "ORG-B-" + suffix, "name", "西城中心"))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        long site = number(adminJson(post("/api/v1/admin/sites"), Map.of(
                "organizationId", firstOrganization, "code", "SITE-" + suffix,
                "name", "第一服务站", "siteType", "SERVICE_STATION"))
                .andExpect(status().isCreated()).andReturn(), "$.id");

        adminJson(post("/api/v1/admin/departments"), Map.of(
                "organizationId", secondOrganization, "siteId", site,
                "code", "BAD-" + suffix, "name", "越界科室"))
                .andExpect(status().isBadRequest());
        adminJson(post("/api/v1/admin/departments"), Map.of(
                "organizationId", firstOrganization, "siteId", site,
                "code", "GP-" + suffix, "name", "全科医学科"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(firstOrganization))
                .andExpect(jsonPath("$.siteId").value(site))
                .andExpect(jsonPath("$.version").doesNotExist());

        long patient = createPatient("identifier-" + suffix);
        String rawIdentifier = "110105-19491231-002x";
        MvcResult identifier = adminJson(post("/api/v1/admin/patients/{id}/identifiers", patient), Map.of(
                        "type", "NATIONAL_ID", "value", rawIdentifier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedValue").value("1101**********002X"))
                .andExpect(jsonPath("$.hash").isString())
                .andExpect(jsonPath("$.value").doesNotExist())
                .andReturn();
        assertThat(body(identifier)).doesNotContain(rawIdentifier).doesNotContain("11010519491231002X");
        assertThat(jdbc.queryForObject("select count(*) from patient_identifier where identifier_hash = ? and masked_value = ?",
                Integer.class, JsonPath.read(body(identifier), "$.hash"), "1101**********002X")).isEqualTo(1);

        assertThat(jdbc.queryForObject("select count(*) from audit_event", Integer.class)).isEqualTo(auditBefore + 6);
    }

    @Test
    void familyProfileIs404UntilVerifiedAndExplicitGrantAlsoAllowsAccess() throws Exception {
        String suffix = suffix();
        long guardian = createPatient("guardian-" + suffix);
        long dependent = createPatient("dependent-" + suffix);
        long grantedPatient = createPatient("granted-" + suffix);

        MvcResult pending = residentJson(post("/api/v1/resident/guardian-relationships"), 901L, guardian,
                        Map.of("dependentPatientId", dependent, "relationshipType", "PARENT",
                                "evidenceReference", "evidence-" + suffix))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long relationshipId = number(pending, "$.id");

        resident(get("/api/v1/resident/family/{patientId}", dependent), 901L, guardian)
                .andExpect(status().isNotFound());
        adminJson(patch("/api/v1/admin/guardian-relationships/{id}/verify", relationshipId), Map.of(
                        "evidenceReference", "verified-" + suffix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED"));

        resident(get("/api/v1/resident/family/{patientId}", dependent), 901L, guardian)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dependent))
                .andExpect(jsonPath("$.name").value("dependent-" + suffix))
                .andExpect(jsonPath("$.idCard").doesNotExist());
        resident(get("/api/v1/resident/family/{patientId}", dependent), 902L, grantedPatient)
                .andExpect(status().isNotFound());

        long grantId = number(adminJson(post("/api/v1/admin/patient-access-grants"), Map.of(
                        "granteeUserId", 902L, "patientId", dependent, "purpose", "FAMILY_SUPPORT",
                        "scopeCode", "BASIC_PROFILE", "validFrom", Instant.now().minusSeconds(60).toString(),
                        "validTo", Instant.now().plusSeconds(3600).toString()))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        resident(get("/api/v1/resident/family/{patientId}", dependent), 902L, grantedPatient)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(dependent));

        adminJson(patch("/api/v1/admin/patient-access-grants/{id}/revoke", grantId), Map.of())
                .andExpect(status().isOk());
        resident(get("/api/v1/resident/family/{patientId}", dependent), 902L, grantedPatient)
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("select count(*) from audit_event where action = 'PATIENT_BASIC_PROFILE_READ' and outcome = 'SUCCESS'", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action = 'PATIENT_BASIC_PROFILE_READ' and outcome = 'DENIED'", Integer.class)).isGreaterThanOrEqualTo(3);
    }

    @Test
    void legacyPatientWriteHashesIdentifierAndNeverReturnsOrStoresPlaintext() throws Exception {
        String raw = "320311-19770706-001x";
        MvcResult result = adminJson(post("/api/patients"), Map.of(
                        "idCard", raw, "name", "legacy-safe-" + suffix(), "gender", "其他",
                        "birthDate", "1977-07-06", "balance", 0))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idCard").doesNotExist())
                .andExpect(jsonPath("$.maskedIdCard").isString()).andReturn();
        long patientId = number(result, "$.id");
        assertThat(body(result)).doesNotContain(raw).doesNotContain("32031119770706001X");
        assertThat(jdbc.queryForObject("select id_card from patient where id = ?", String.class, patientId))
                .doesNotContain(raw).contains("*");
        assertThat(jdbc.queryForObject("select count(*) from patient_identifier where patient_id = ? and identifier_hash is not null", Integer.class, patientId)).isEqualTo(1);

        String masked = JsonPath.read(body(result), "$.maskedIdCard");
        adminJson(put("/api/patients/{id}", patientId), Map.of(
                        "name", "demographics-only", "gender", "其他",
                        "birthDate", "1977-07-07", "phone", "13800138000", "balance", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedIdCard").value(masked));
        assertThat(jdbc.queryForObject("select count(*) from patient_identifier where patient_id = ?", Integer.class, patientId))
                .isEqualTo(1);

        adminJson(get("/api/patients").param("keyword", raw).param("size", "100"), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.hasItem((int) patientId)));
    }

    private long createPatient(String name) throws Exception {
        String suffix = suffix();
        return number(adminJson(post("/api/patients"), Map.of(
                        "idCard", "R1" + suffix, "name", name, "gender", "其他",
                        "birthDate", LocalDate.of(1990, 1, 2).toString(), "balance", 0))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions adminJson(
            MockHttpServletRequestBuilder request, Object value) throws Exception {
        return mvc.perform(request.with(jwt().jwt(builder -> builder.subject("r1-admin")
                        .claim("roles", List.of("ADMIN")).claim("role", "ADMIN")
                        .claim("mustChangePassword", false))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(value)));
    }

    private org.springframework.test.web.servlet.ResultActions residentJson(
            MockHttpServletRequestBuilder request, long userId, long patientId, Object value) throws Exception {
        return resident(request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(value)), userId, patientId);
    }

    private org.springframework.test.web.servlet.ResultActions resident(
            MockHttpServletRequestBuilder request, long userId, long patientId) throws Exception {
        return mvc.perform(request.with(jwt().jwt(builder -> builder.subject("r1-resident-" + userId)
                        .claim("roles", List.of("RESIDENT")).claim("role", "RESIDENT")
                        .claim("userId", userId).claim("patientId", patientId)
                        .claim("subjectId", patientId).claim("mustChangePassword", false))
                .authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))));
    }

    private long number(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(body(result), path)).longValue();
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
