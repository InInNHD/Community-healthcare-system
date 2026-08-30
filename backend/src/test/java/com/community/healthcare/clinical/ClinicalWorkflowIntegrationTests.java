package com.community.healthcare.clinical;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * API-level regression gates for the clinical workflows shared by all three portals.
 * Fixtures use unique business keys so the tests are independent and safe to run in any order.
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:clinical-workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=0")
@ActiveProfiles("demo")
class ClinicalWorkflowIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void appointmentFollowsStrictStateMachineAndPortalPermissions() throws Exception {
        PortalSession resident = login("resident", "Resident@123456", "resident");
        PortalSession staff = login("doctor", "Doctor@123456", "staff");
        String suffix = suffix();
        long doctorId = staff.staffId();

        MvcResult created = performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(3),
                        "reason", "state-machine-" + suffix))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.patientId").value(resident.patientId()))
                .andReturn();
        long appointmentId = number(created, "$.id");

        performJson(patch("/api/staff/appointments/{id}/status", appointmentId), resident.token(),
                Map.of("status", "CONFIRMED")).andExpect(status().isForbidden());
        performJson(patch("/api/appointments/{id}/status", appointmentId), staff.token(),
                Map.of("status", "CONFIRMED")).andExpect(status().isForbidden());

        // Skipping confirmation and moving backwards are both forbidden.
        performJson(patch("/api/staff/appointments/{id}/status", appointmentId), staff.token(),
                Map.of("status", "COMPLETED")).andExpect(status().isBadRequest());
        updateAppointmentStatus(staff.token(), appointmentId, "CONFIRMED", 200);
        updateAppointmentStatus(staff.token(), appointmentId, "PENDING", 400);
        updateAppointmentStatus(staff.token(), appointmentId, "COMPLETED", 200);

        // Terminal states are immutable, including through the resident cancellation endpoint.
        updateAppointmentStatus(staff.token(), appointmentId, "CANCELLED", 400);
        authorized(patch("/api/resident/appointments/{id}/cancel", appointmentId), resident.token())
                .andExpect(status().isBadRequest());

        MvcResult cancellable = performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(4),
                        "reason", "resident-cancel-" + suffix))
                .andExpect(status().isCreated()).andReturn();
        long cancellableId = number(cancellable, "$.id");
        authorized(patch("/api/resident/appointments/{id}/cancel", cancellableId), resident.token())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        updateAppointmentStatus(staff.token(), cancellableId, "CONFIRMED", 400);
    }

    @Test
    void pastConfirmedAppointmentCanBeCompletedWithoutRevalidatingItsOriginalSchedule() throws Exception {
        PortalSession resident = login("resident", "Resident@123456", "resident");
        PortalSession staff = login("doctor", "Doctor@123456", "staff");

        MvcResult created = performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "doctorId", staff.staffId(),
                        "scheduledAt", futureTime(2),
                        "reason", "past-completion-" + suffix()))
                .andExpect(status().isCreated())
                .andReturn();
        long appointmentId = number(created, "$.id");

        updateAppointmentStatus(staff.token(), appointmentId, "CONFIRMED", 200);
        jdbc.update("update appointment set scheduled_at = ? where id = ?",
                LocalDateTime.now().minusHours(1), appointmentId);

        updateAppointmentStatus(staff.token(), appointmentId, "COMPLETED", 200);
    }

    @Test
    void adminCannotCreateAppointmentInThePast() throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        long patientId = createPatient(admin.token(), suffix());
        String doctorSuffix = suffix();
        long doctorId = createDoctor(admin.token(), doctorSuffix, "past-validation-doctor-" + doctorSuffix);

        performJson(post("/api/appointments"), admin.token(), Map.of(
                        "patientId", patientId,
                        "doctorId", doctorId,
                        "scheduledAt", LocalDateTime.now().minusMinutes(5).withNano(0).toString(),
                        "reason", "invalid-past-appointment"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void residentCannotCreateAndAdminCannotRescheduleAppointmentInThePast() throws Exception {
        PortalSession resident = login("resident", "Resident@123456", "resident");
        PortalSession admin = login("admin", "Admin@123456", "admin");
        long doctorId = firstId(getJson("/api/resident/doctors?size=10", resident.token()));

        performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "doctorId", doctorId,
                        "scheduledAt", LocalDateTime.now().minusMinutes(1).withNano(0).toString(),
                        "reason", "resident-past-appointment"))
                .andExpect(status().isBadRequest());

        MvcResult created = performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(2),
                        "reason", "admin-reschedule-appointment"))
                .andExpect(status().isCreated()).andReturn();
        performJson(put("/api/appointments/{id}", number(created, "$.id")), admin.token(), Map.of(
                        "patientId", resident.patientId(),
                        "doctorId", doctorId,
                        "scheduledAt", LocalDateTime.now().minusMinutes(1).withNano(0).toString(),
                        "reason", "admin-invalid-reschedule"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void residentQueriesAndWritesAreAlwaysScopedToAuthenticatedPatient() throws Exception {
        PortalSession resident = login("resident", "Resident@123456", "resident");
        PortalSession admin = login("admin", "Admin@123456", "admin");
        String suffix = suffix();
        long foreignPatientId = createPatient(admin.token(), suffix);
        long doctorId = firstId(getJson("/api/resident/doctors?size=10", resident.token()));

        MvcResult foreignAppointment = performJson(post("/api/appointments"), admin.token(), Map.of(
                        "patientId", foreignPatientId,
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(5),
                        "reason", "foreign-appointment-" + suffix))
                .andExpect(status().isCreated()).andReturn();
        long foreignAppointmentId = number(foreignAppointment, "$.id");

        performJson(post("/api/health-records"), admin.token(), Map.of(
                        "patientId", foreignPatientId,
                        "recordedAt", LocalDateTime.now().withNano(0).toString(),
                        "heartRate", 81,
                        "systolicPressure", 128,
                        "diastolicPressure", 82,
                        "note", "foreign-health-" + suffix))
                .andExpect(status().isCreated());
        performJson(post("/api/chronic-cases"), admin.token(), Map.of(
                        "patientId", foreignPatientId,
                        "diseaseType", "foreign-disease-" + suffix,
                        "riskLevel", "中风险",
                        "diagnosisDate", LocalDate.now().minusYears(1).toString(),
                        "doctorId", doctorId,
                        "managementPlan", "foreign-plan-" + suffix))
                .andExpect(status().isCreated());

        MvcResult ownAppointment = performJson(post("/api/resident/appointments"), resident.token(), Map.of(
                        "patientId", foreignPatientId,
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(6),
                        "reason", "forced-own-appointment-" + suffix))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(resident.patientId()))
                .andReturn();
        long ownAppointmentId = number(ownAppointment, "$.id");

        MvcResult ownHealth = performJson(post("/api/resident/health-records"), resident.token(), Map.of(
                        "patientId", foreignPatientId,
                        "recordedAt", LocalDateTime.now().plusMinutes(1).withNano(0).toString(),
                        "heartRate", 72,
                        "systolicPressure", 121,
                        "diastolicPressure", 79,
                        "bloodOxygen", 98,
                        "note", "forced-own-health-" + suffix))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(resident.patientId()))
                .andReturn();
        long ownHealthId = number(ownHealth, "$.id");

        String appointments = getJson("/api/resident/appointments?page=0&size=100", resident.token());
        assertThat(numbers(appointments, "$.items[*].patientId")).containsOnly(resident.patientId());
        assertThat(numbers(appointments, "$.items[*].id"))
                .contains(ownAppointmentId).doesNotContain(foreignAppointmentId);

        // A forged patientId query parameter must not widen resident access.
        String health = getJson("/api/resident/health-records?page=0&size=100&patientId=" + foreignPatientId,
                resident.token());
        assertThat(numbers(health, "$.items[*].patientId")).containsOnly(resident.patientId());
        assertThat(numbers(health, "$.items[*].id")).contains(ownHealthId);

        String chronicPlans = getJson("/api/resident/chronic-plans", resident.token());
        assertThat(numbers(chronicPlans, "$[*].patientId")).containsOnly(resident.patientId());
        assertThat(chronicPlans).doesNotContain("foreign-plan-" + suffix);

        authorized(patch("/api/resident/appointments/{id}/cancel", foreignAppointmentId), resident.token())
                .andExpect(status().isNotFound());
        authorized(get("/api/resident/overview"), resident.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value(resident.patientId()))
                .andExpect(jsonPath("$.latestHealthRecord.patientId").value(resident.patientId()));
    }

    @Test
    void healthRecordOwnershipRequiresAnActivePatient() throws Exception {
        PortalSession resident = login("resident", "Resident@123456", "resident");
        PortalSession staff = login("nurse", "Nurse@123456", "staff");
        PortalSession admin = login("admin", "Admin@123456", "admin");
        String suffix = suffix();
        long inactivePatientId = createPatient(admin.token(), suffix);

        authorized(delete("/api/patients/{id}", inactivePatientId), admin.token())
                .andExpect(status().isOk());
        performJson(post("/api/staff/health-records"), staff.token(), Map.of(
                        "patientId", inactivePatientId,
                        "heartRate", 70,
                        "note", "inactive-patient-" + suffix))
                .andExpect(status().isNotFound());

        performJson(post("/api/resident/health-records"), resident.token(), Map.of(
                        "patientId", inactivePatientId,
                        "heartRate", 71,
                        "note", "resident-owned-" + suffix))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(resident.patientId()));
    }

    @Test
    void doctorsOnlySeeAndChangeTheirOwnAppointmentsWhileNursesUseTheTeamScope() throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        PortalSession doctorOne = login("doctor", "Doctor@123456", "staff");
        PortalSession nurse = login("nurse", "Nurse@123456", "staff");
        String suffix = suffix();
        long patientId = createPatient(admin.token(), suffix);
        Long demoSiteId = jdbc.queryForObject("select id from site where code='DEMO-SITE'", Long.class);
        jdbc.update("insert into patient_site_enrollment(patient_id,site_id,enrolled_at,active,created_at) "
                        + "values(?,?,current_timestamp,true,current_timestamp)",
                patientId, demoSiteId);
        long doctorTwoId = createDoctor(admin.token(), suffix, "scoped-doctor-" + suffix);

        long doctorOneAppointment = number(performJson(post("/api/appointments"), admin.token(), Map.of(
                        "patientId", patientId,
                        "doctorId", doctorOne.staffId(),
                        "scheduledAt", futureTime(8),
                        "reason", "doctor-one-scope-" + suffix))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        long doctorTwoAppointment = number(performJson(post("/api/appointments"), admin.token(), Map.of(
                        "patientId", patientId,
                        "doctorId", doctorTwoId,
                        "scheduledAt", futureTime(9),
                        "reason", "doctor-two-scope-" + suffix))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        // Keep the summary assertion independent from seed data and from tests running across midnight.
        jdbc.update("update appointment set scheduled_at = current_timestamp where id = ?", doctorOneAppointment);

        String doctorOnePage = getJson("/api/staff/appointments?page=0&size=100", doctorOne.token());
        assertThat(numbers(doctorOnePage, "$.items[*].id"))
                .contains(doctorOneAppointment).doesNotContain(doctorTwoAppointment);

        String doctorTwoPage = body(asStaff(get("/api/staff/appointments?page=0&size=100"),
                doctorTwoId, "DOCTOR").andExpect(status().isOk()).andReturn());
        assertThat(numbers(doctorTwoPage, "$.items[*].id"))
                .doesNotContain(doctorTwoAppointment, doctorOneAppointment);

        performJson(patch("/api/staff/appointments/{id}/status", doctorTwoAppointment), doctorOne.token(),
                Map.of("status", "CONFIRMED")).andExpect(status().isNotFound());
        asStaff(patch("/api/staff/appointments/{id}/status", doctorOneAppointment)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "CONFIRMED"))),
                doctorTwoId, "DOCTOR").andExpect(status().isNotFound());

        // A provisioned nurse sees the current station scope, while unprovisioned staff above fail closed.
        String nursePage = getJson("/api/staff/appointments?page=0&size=100", nurse.token());
        assertThat(numbers(nursePage, "$.items[*].id"))
                .contains(doctorOneAppointment, doctorTwoAppointment);
        performJson(patch("/api/staff/appointments/{id}/status", doctorTwoAppointment), nurse.token(),
                Map.of("status", "CONFIRMED"))
                .andExpect(status().isOk());

        String nurseSummary = getJson("/api/staff/summary", nurse.token());
        assertThat(((Number) JsonPath.read(nurseSummary, "$.appointmentsToday")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(nurseSummary, "$.pendingAppointments")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(nurseSummary, "$.chronicCases")).longValue()).isGreaterThanOrEqualTo(1);

        String doctorPatients = getJson("/api/staff/patients?page=0&size=100", doctorOne.token());
        assertThat(numbers(doctorPatients, "$.items[*].id")).contains(patientId);
        assertThat(doctorPatients).doesNotContain("idCard");
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action = 'STAFF_PATIENT_QUERY'", Integer.class))
                .isGreaterThanOrEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"低风险", "中风险", "高风险"})
    void standardChronicRiskLevelsAreAccepted(String riskLevel) throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        String suffix = suffix();
        long patientId = createPatient(admin.token(), suffix);

        performJson(post("/api/chronic-cases"), admin.token(), Map.of(
                        "patientId", patientId,
                        "diseaseType", "risk-white-list-" + suffix,
                        "riskLevel", riskLevel,
                        "diagnosisDate", LocalDate.now().minusMonths(1).toString(),
                        "managementPlan", "standard risk plan"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskLevel").value(riskLevel));
    }

    @Test
    void nonStandardChronicRiskLevelIsRejectedWithoutPersistence() throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        String suffix = suffix();
        long patientId = createPatient(admin.token(), suffix);
        String diseaseType = "invalid-risk-" + suffix;

        performJson(post("/api/chronic-cases"), admin.token(), Map.of(
                        "patientId", patientId,
                        "diseaseType", diseaseType,
                        "riskLevel", "极高风险",
                        "diagnosisDate", LocalDate.now().minusMonths(1).toString(),
                        "managementPlan", "must not persist"))
                .andExpect(status().isBadRequest());

        String page = getJson("/api/chronic-cases?keyword=" + diseaseType, admin.token());
        assertThat(numbers(page, "$.items[*].patientId")).isEmpty();
    }

    @Test
    void stockDeltaIsAtomicNeverNegativeAndDrivesLowStockAlerts() throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        PortalSession staff = login("doctor", "Doctor@123456", "staff");
        String suffix = suffix();
        String medicineName = "atomic-stock-" + suffix;
        long medicineId = createMedicine(admin.token(), medicineName, 5, 2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> stockDeltaStatus(
                    admin.token(), medicineId, -4, ready, start));
            Future<Integer> second = executor.submit(() -> stockDeltaStatus(
                    admin.token(), medicineId, -4, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 400);
        } finally {
            executor.shutdownNow();
        }

        assertThat(medicineStock(admin.token(), medicineName, medicineId)).isEqualTo(1);
        String alerts = getJson("/api/staff/medicine-alerts", staff.token());
        assertThat(numbers(alerts, "$[*].id")).contains(medicineId);

        performJson(patch("/api/medicines/{id}/stock", medicineId), admin.token(), Map.of("delta", 5))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(6));
        assertThat(numbers(getJson("/api/staff/medicine-alerts", staff.token()), "$[*].id"))
                .doesNotContain(medicineId);

        performJson(patch("/api/medicines/{id}/stock", medicineId), admin.token(), Map.of("delta", -7))
                .andExpect(status().isBadRequest());
        assertThat(medicineStock(admin.token(), medicineName, medicineId)).isEqualTo(6);
    }

    @Test
    void everyDeleteIsLogicalAndDeletedRowsAreHiddenFromQueries() throws Exception {
        PortalSession admin = login("admin", "Admin@123456", "admin");
        String suffix = suffix();
        String patientName = "soft-patient-" + suffix;
        String doctorName = "soft-doctor-" + suffix;
        String medicineName = "soft-medicine-" + suffix;
        String appointmentReason = "soft-appointment-" + suffix;
        String diseaseType = "soft-chronic-" + suffix;

        long patientId = createPatient(admin.token(), suffix, patientName);
        long doctorId = createDoctor(admin.token(), suffix, doctorName);
        long medicineId = createMedicine(admin.token(), medicineName, 3, 5);
        long appointmentId = number(performJson(post("/api/appointments"), admin.token(), Map.of(
                        "patientId", patientId,
                        "doctorId", doctorId,
                        "scheduledAt", futureTime(7),
                        "reason", appointmentReason))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        long healthRecordId = number(performJson(post("/api/health-records"), admin.token(), Map.of(
                        "patientId", patientId,
                        "recordedAt", LocalDateTime.now().withNano(0).toString(),
                        "heartRate", 74,
                        "note", "soft-health-" + suffix))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        long chronicCaseId = number(performJson(post("/api/chronic-cases"), admin.token(), Map.of(
                        "patientId", patientId,
                        "diseaseType", diseaseType,
                        "riskLevel", "低风险",
                        "diagnosisDate", LocalDate.now().minusMonths(2).toString(),
                        "managementPlan", "soft delete plan"))
                .andExpect(status().isCreated()).andReturn(), "$.id");

        authorized(delete("/api/appointments/{id}", appointmentId), admin.token()).andExpect(status().isOk());
        authorized(delete("/api/health-records/{id}", healthRecordId), admin.token()).andExpect(status().isOk());
        authorized(delete("/api/chronic-cases/{id}", chronicCaseId), admin.token()).andExpect(status().isOk());
        authorized(delete("/api/medicines/{id}", medicineId), admin.token()).andExpect(status().isOk());
        authorized(delete("/api/doctors/{id}", doctorId), admin.token()).andExpect(status().isOk());
        authorized(delete("/api/patients/{id}", patientId), admin.token()).andExpect(status().isOk());

        // Logical deletion retains every database row for audit/recovery.
        assertPhysicalRowExists("appointment", appointmentId);
        assertPhysicalRowExists("health_record", healthRecordId);
        assertPhysicalRowExists("chronic_case", chronicCaseId);
        assertPhysicalRowExists("medicine", medicineId);
        assertPhysicalRowExists("doctor", doctorId);
        assertPhysicalRowExists("patient", patientId);

        assertThat(numbers(getJson("/api/appointments?keyword=" + appointmentReason, admin.token()), "$.items[*].id"))
                .doesNotContain(appointmentId);
        assertThat(numbers(getJson("/api/health-records?patientId=" + patientId + "&size=100", admin.token()), "$.items[*].id"))
                .doesNotContain(healthRecordId);
        assertThat(numbers(getJson("/api/chronic-cases?keyword=" + diseaseType, admin.token()), "$.items[*].id"))
                .doesNotContain(chronicCaseId);
        assertThat(numbers(getJson("/api/medicines?keyword=" + medicineName, admin.token()), "$.items[*].id"))
                .doesNotContain(medicineId);
        assertThat(numbers(getJson("/api/doctors?keyword=" + doctorName, admin.token()), "$.items[*].id"))
                .doesNotContain(doctorId);
        assertThat(numbers(getJson("/api/patients?keyword=" + patientName, admin.token()), "$.items[*].id"))
                .doesNotContain(patientId);
    }

    private PortalSession login(String username, String password, String portal) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password, "portal", portal))))
                .andExpect(status().isOk()).andReturn();
        String body = body(result);
        Number patientId = JsonPath.read(body, "$.patientId");
        Number staffId = JsonPath.read(body, "$.staffId");
        return new PortalSession(JsonPath.read(body, "$.accessToken"),
                patientId == null ? null : patientId.longValue(),
                staffId == null ? null : staffId.longValue());
    }

    private long createPatient(String token, String suffix) throws Exception {
        return createPatient(token, suffix, "isolation-patient-" + suffix);
    }

    private long createPatient(String token, String suffix, String name) throws Exception {
        return number(performJson(post("/api/patients"), token, Map.of(
                        "idCard", "T" + suffix,
                        "name", name,
                        "gender", "其他",
                        "birthDate", "1992-03-04",
                        "phone", "13900000000",
                        "address", "integration test address",
                        "balance", 0))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private long createDoctor(String token, String suffix, String name) throws Exception {
        return number(performJson(post("/api/doctors"), token, Map.of(
                        "employeeNo", "E" + suffix,
                        "name", name,
                        "department", "全科医学科",
                        "title", "主治医师",
                        "specialty", "integration tests"))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private long createMedicine(String token, String name, int stock, int minimumStock) throws Exception {
        return number(performJson(post("/api/medicines"), token, Map.of(
                        "name", name,
                        "category", "test",
                        "price", 12.5,
                        "stock", stock,
                        "minimumStock", minimumStock,
                        "specification", "integration test"))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private int stockDeltaStatus(String token, long medicineId, int delta,
                                 CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) return 599;
        return performJson(patch("/api/medicines/{id}/stock", medicineId), token, Map.of("delta", delta))
                .andReturn().getResponse().getStatus();
    }

    private int medicineStock(String token, String keyword, long medicineId) throws Exception {
        String response = getJson("/api/medicines?keyword=" + keyword, token);
        List<Number> values = JsonPath.read(response, "$.items[?(@.id == " + medicineId + ")].stock");
        assertThat(values).hasSize(1);
        return values.get(0).intValue();
    }

    private void updateAppointmentStatus(String token, long id, String target, int expectedStatus) throws Exception {
        performJson(patch("/api/staff/appointments/{id}/status", id), token, Map.of("status", target))
                .andExpect(status().is(expectedStatus));
    }

    private void assertPhysicalRowExists(String table, long id) {
        Integer count = jdbc.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
        assertThat(count).as("soft-deleted row in %s", table).isEqualTo(1);
    }

    private long firstId(String page) {
        List<Number> ids = JsonPath.read(page, "$.items[*].id");
        assertThat(ids).isNotEmpty();
        return ids.get(0).longValue();
    }

    private long number(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(body(result), path)).longValue();
    }

    private List<Long> numbers(String response, String path) {
        List<Number> values = JsonPath.read(response, path);
        return values.stream().map(Number::longValue).toList();
    }

    private String getJson(String path, String token) throws Exception {
        return body(authorized(get(path), token).andExpect(status().isOk()).andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions performJson(
            MockHttpServletRequestBuilder request, String token, Object content) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(content)));
    }

    private org.springframework.test.web.servlet.ResultActions authorized(
            MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions asStaff(
            MockHttpServletRequestBuilder request, long staffId, String role) throws Exception {
        return mvc.perform(request.with(jwt().jwt(builder -> builder
                        .subject("integration-" + role.toLowerCase() + "-" + staffId)
                        .claim("roles", List.of(role))
                        .claim("role", role)
                        .claim("portal", "staff")
                        .claim("staffId", staffId)
                        .claim("subjectId", staffId)
                        .claim("mustChangePassword", false))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String futureTime(int days) {
        return LocalDateTime.now().plusDays(days).withNano(0).toString();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record PortalSession(String token, Long patientId, Long staffId) {}
}
