package com.community.healthcare;

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
import jakarta.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:demo-profile;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=0")
@ActiveProfiles("demo")
class DemoProfileSecurityIntegrationTests {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sharedDemoAccountsRemainUsableWithoutChangingThePrefilledPassword() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"resident\",\"password\":\"Resident@123456\","
                                + "\"portal\":\"resident\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/resident/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void staleAccessCookieDoesNotBlockFreshLogin() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("healthcare_access", "stale-invalid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\","
                                + "\"portal\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("healthcare_access=")));
    }

    @Test
    void loginIssuesHttpOnlyCookieAndCookieAuthenticationRequiresCsrfForWrites() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"resident\",\"password\":\"Resident@123456\","
                                + "\"portal\":\"resident\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("healthcare_access="),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))))
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Cookie accessCookie = new Cookie("healthcare_access", token);

        mvc.perform(get("/api/resident/overview").cookie(accessCookie))
                .andExpect(status().isOk());
        mvc.perform(post("/api/resident/health-records").cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\":72}"))
                .andExpect(status().isForbidden());

        MvcResult csrf = mvc.perform(get("/api/auth/csrf").cookie(accessCookie))
                .andExpect(status().isOk()).andReturn();
        String csrfToken = JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/resident/health-records")
                        .cookie(accessCookie, new Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\":72}"))
                .andExpect(status().isCreated());
    }

    @Test
    void actuatorMetricsAreRestrictedToAdministrators() throws Exception {
        String residentToken = loginToken("resident", "Resident@123456", "resident");
        String adminToken = loginToken("admin", "Admin@123456", "admin");

        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void exposesPortalContactConfigurationWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/public/portal-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("社区卫生服务中心"))
                .andExpect(jsonPath("$.servicePhone").value("010-12345678"))
                .andExpect(jsonPath("$.serviceHours").value("工作日 08:00-17:00"))
                .andExpect(jsonPath("$.emergencyPhone").value("120"));
    }

    @Test
    void openApiContractIsAvailableOnlyToAdministrators() throws Exception {
        String residentToken = loginToken("resident", "Resident@123456", "resident");
        String adminToken = loginToken("admin", "Admin@123456", "admin");

        mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }

    private String loginToken(String username, String password, String portal) throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                                + "\",\"portal\":\"" + portal + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }
}
