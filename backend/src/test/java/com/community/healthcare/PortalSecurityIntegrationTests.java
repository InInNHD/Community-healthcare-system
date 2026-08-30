package com.community.healthcare;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.security.bootstrap.must-change-password=true",
        "spring.datasource.url=jdbc:h2:mem:portal-security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=0"
})
@ActiveProfiles("demo")
class PortalSecurityIntegrationTests {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void eachAccountCanOnlyLogInThroughItsOwnPortal() throws Exception {
        login("admin", "Admin@123456", "admin")
                .andExpect(status().isOk()).andExpect(jsonPath("$.portal").value("admin"));
        login("doctor", "Doctor@123456", "staff")
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("DOCTOR"))
                .andExpect(jsonPath("$.staffId").isNumber());
        login("nurse", "Nurse@123456", "staff")
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("NURSE"));
        login("resident", "Resident@123456", "resident")
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").isNumber());

        login("resident", "Resident@123456", "admin").andExpect(status().isUnauthorized());
        login("doctor", "Doctor@123456", "resident").andExpect(status().isUnauthorized());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void residentCannotUseAdminApiOrSubmitAnotherPatientId() throws Exception {
        MvcResult login = login("resident", "Resident@123456", "resident")
                .andExpect(status().isOk()).andReturn();
        String initialToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Number patientId = JsonPath.read(login.getResponse().getContentAsString(), "$.patientId");
        mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Resident@123456\",\"newPassword\":\"Resident@654321\"}"))
                .andExpect(status().isOk());
        MvcResult relogin = login("resident", "Resident@654321", "resident")
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(relogin.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/resident/health-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":999999,\"heartRate\":72,\"note\":\"self report\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientId.longValue()));
    }

    @Test
    void tokenContainsValidatedIssuerAudienceAndActiveKid() throws Exception {
        MvcResult login = login("doctor", "Doctor@123456", "staff")
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        SignedJWT jwt = SignedJWT.parse(token);

        assertEquals("demo-active-2026", jwt.getHeader().getKeyID());
        assertEquals("community-healthcare", jwt.getJWTClaimsSet().getIssuer());
        assertTrue(jwt.getJWTClaimsSet().getAudience().contains("community-healthcare-web"));
    }

    @Test
    void exposesPasswordPolicyWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/auth/password-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLength").value(12))
                .andExpect(jsonPath("$.maxLength").value(128))
                .andExpect(jsonPath("$.maxUtf8Bytes").value(72))
                .andExpect(jsonPath("$.requireUppercase").value(true))
                .andExpect(jsonPath("$.requireLowercase").value(true))
                .andExpect(jsonPath("$.requireDigit").value(true))
                .andExpect(jsonPath("$.requireSpecial").value(true));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void passwordChangeEnforcesPolicyClearsFlagAndInvalidatesOldToken() throws Exception {
        MvcResult login = login("resident", "Resident@123456", "resident")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();
        String oldToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        assertEquals(0L, SignedJWT.parse(oldToken).getJWTClaimsSet().getLongClaim("pwdVersion"));

        mvc.perform(get("/api/resident/profile").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Resident@123456\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Resident@123456\",\"newPassword\":\"Resident@654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.reauthenticationRequired").value(true));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        login("resident", "Resident@123456", "resident").andExpect(status().isUnauthorized());
        MvcResult relogin = login("resident", "Resident@654321", "resident")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();
        String newToken = JsonPath.read(relogin.getResponse().getContentAsString(), "$.accessToken");
        assertEquals(1L, SignedJWT.parse(newToken).getJWTClaimsSet().getLongClaim("pwdVersion"));
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password, String portal) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                        + "\",\"portal\":\"" + portal + "\"}"));
    }
}
