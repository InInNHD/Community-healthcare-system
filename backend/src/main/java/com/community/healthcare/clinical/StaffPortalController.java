package com.community.healthcare.clinical;

import com.community.healthcare.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 医生和护士使用的日常工作门户接口。
 *
 * <p>Controller 从 JWT 构造 {@link StaffAccessScope}，查询与写入均在机构站点范围内执行；
 * 管理端全量 CRUD 接口不应复用于这里。</p>
 */
@RestController
@RequestMapping("/api/staff")
class StaffPortalController {
    private final StaffPortalQueryService queries;
    private final StaffPortalCommandService commands;
    private final AppointmentApplicationService appointments;

    StaffPortalController(StaffPortalQueryService queries, StaffPortalCommandService commands,
                          AppointmentApplicationService appointments) {
        this.queries = queries;
        this.commands = commands;
        this.appointments = appointments;
    }

    @GetMapping("/summary")
    StaffSummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return queries.summary(StaffAccessScope.from(jwt));
    }

    @GetMapping("/appointments")
    PageResponse<AppointmentResponse> appointments(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "false") boolean today,
                                                   @RequestParam(defaultValue = "") String keyword,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return queries.appointments(StaffAccessScope.from(jwt), today, keyword, page, size);
    }

    @PatchMapping("/appointments/{id}/status")
    AppointmentResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody AppointmentStatusRequest request) {
        StaffAccessScope scope = StaffAccessScope.from(jwt);
        return appointments.changeStatusForStaff(id, request.status(), scope.appointmentDoctorId(),
                queries.scopedPatientIds(scope));
    }

    @GetMapping("/patients")
    PageResponse<PatientResponse> patients(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "") String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return queries.patients(StaffAccessScope.from(jwt), keyword, page, size);
    }

    @GetMapping("/health-records")
    PageResponse<HealthRecordResponse> healthRecords(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(required = false) Long patientId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return queries.healthRecords(StaffAccessScope.from(jwt), patientId, page, size);
    }

    @PostMapping("/health-records")
    @ResponseStatus(HttpStatus.CREATED)
    HealthRecordResponse createHealthRecord(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody StaffHealthRecordRequest request) {
        return commands.createHealthRecord(StaffAccessScope.from(jwt), request);
    }

    @GetMapping("/chronic-cases")
    PageResponse<ChronicCaseResponse> chronicCases(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "") String keyword,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return queries.chronicCases(StaffAccessScope.from(jwt), keyword, page, size);
    }

    @GetMapping({"/medicine-alerts", "/medicines"})
    List<MedicineResponse> medicineAlerts() {
        return queries.medicineAlerts();
    }
}
