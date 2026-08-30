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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 居民健康门户的自助服务接口。
 *
 * <p>所有个人数据操作都从 JWT 获取 patientId，居民无法通过路径或请求体切换到其他人的档案；
 * 医生列表是预约辅助信息，不构成互联网诊疗入口。</p>
 */
@RestController
@RequestMapping("/api/resident")
class ResidentPortalController {
    private final ResidentPortalQueryService queries;
    private final ResidentPortalCommandService commands;
    private final AppointmentApplicationService appointments;

    ResidentPortalController(ResidentPortalQueryService queries, ResidentPortalCommandService commands,
                             AppointmentApplicationService appointments) {
        this.queries = queries;
        this.commands = commands;
        this.appointments = appointments;
    }

    @GetMapping({"/overview", "/summary"})
    ResidentOverviewResponse overview(@AuthenticationPrincipal Jwt jwt) {
        return queries.overview(patientId(jwt));
    }

    @GetMapping("/profile")
    PatientResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return queries.profile(patientId(jwt));
    }

    @PutMapping("/profile")
    PatientResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ResidentProfileRequest request) {
        return commands.updateProfile(patientId(jwt), request);
    }

    @GetMapping("/appointments")
    PageResponse<AppointmentResponse> appointments(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return queries.appointments(patientId(jwt), page, size);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentResponse createAppointment(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody ResidentAppointmentRequest request) {
        return commands.createAppointment(patientId(jwt), request);
    }

    @PatchMapping("/appointments/{id}/cancel")
    AppointmentResponse cancelAppointment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return appointments.cancelOwned(id, patientId(jwt));
    }

    @GetMapping("/health-records")
    PageResponse<HealthRecordResponse> healthRecords(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return queries.healthRecords(patientId(jwt), page, size);
    }

    @PostMapping("/health-records")
    @ResponseStatus(HttpStatus.CREATED)
    HealthRecordResponse createHealthRecord(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody ResidentHealthRecordRequest request) {
        return commands.createHealthRecord(patientId(jwt), request);
    }

    @GetMapping({"/chronic-plans", "/chronic-cases"})
    List<ChronicCaseResponse> chronicPlans(@AuthenticationPrincipal Jwt jwt) {
        return queries.chronicPlans(patientId(jwt));
    }

    @GetMapping("/doctors")
    PageResponse<DoctorResponse> doctors(@RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return queries.doctors(keyword, page, size);
    }

    private Long patientId(Jwt jwt) {
        return PortalClaims.requiredLong(jwt, "patientId");
    }
}
