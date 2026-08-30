package com.community.healthcare.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mfa-login;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.security.bootstrap.enabled=true",
        "app.security.bootstrap.must-change-password=false",
        "app.security.bootstrap.admin-password=Admin@123456",
        "app.security.bootstrap.doctor-password=Doctor@123456",
        "app.security.bootstrap.nurse-password=Nurse@123456",
        "app.security.bootstrap.resident-password=Resident@123456",
        "app.security.mfa.enabled=true",
        "app.security.mfa.encryption-key=test-mfa-encryption-key-with-at-least-32-bytes"
})
@ActiveProfiles("test")
class MfaLoginIntegrationTests {
    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @Autowired WebApplicationContext context;
    @Autowired AppUserRepository users;
    @Autowired MfaAuthenticationService mfa;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        AppUser admin = users.findByUsername("admin").orElseThrow();
        if (admin.getMfaSecretCiphertext() == null) {
            admin.enrollMfa(mfa.encryptForProvisioning(SECRET));
            users.save(admin);
        }
    }

    @Test
    void privilegedLoginRequiresAOneTimeMfaChallengeBeforeIssuingJwt() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\",\"portal\":\"staff\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult challengeResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\",\"portal\":\"admin\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.challengeToken").isString())
                .andReturn();

        String body = challengeResult.getResponse().getContentAsString();
        assertThat(body).doesNotContain(SECRET).doesNotContain("mfaSecret");
        String challenge = JsonPath.read(body, "$.challengeToken");
        String code = mfa.currentCodeForTesting(SECRET);

        mvc.perform(post("/api/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"" + challenge + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.portal").value("admin"));

        mvc.perform(post("/api/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"" + challenge + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mfaAssuredAdministratorCanProvisionAStaffSecretExactlyOnceWithAudit() throws Exception {
        AppUser pharmacist = users.findByUsername("pharmacist").orElseGet(() -> users.save(
                new AppUser("pharmacist", encoder.encode("Pharmacist@123456"), "药师",
                        AppRole.PHARMACIST, 9L, null, false)));
        String token = adminToken();

        MvcResult provisioned = mvc.perform(post("/api/v1/admin/accounts/pharmacist/mfa/provision")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secret").isString())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andReturn();
        String secret = JsonPath.read(provisioned.getResponse().getContentAsString(), "$.secret");
        assertThat(users.findById(pharmacist.getId()).orElseThrow().getMfaSecretCiphertext())
                .doesNotContain(secret);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action='MFA_PROVISION'", Integer.class))
                .isPositive();

        mvc.perform(post("/api/v1/admin/accounts/pharmacist/mfa/provision")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String adminToken() throws Exception {
        MvcResult challengeResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\",\"portal\":\"admin\"}"))
                .andExpect(status().isAccepted()).andReturn();
        String challenge = JsonPath.read(challengeResult.getResponse().getContentAsString(), "$.challengeToken");
        String code = mfa.currentCodeForTesting(SECRET);
        MvcResult verified = mvc.perform(post("/api/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"" + challenge + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
    }
}
