package com.community.healthcare.scheduling.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import static com.community.healthcare.scheduling.infrastructure.R2ClinicalApplicationService.*;

/** 居民端排班查询、预约和取消预约入口。 */
@RestController @RequestMapping("/api/v1/resident/scheduling")
class ResidentSchedulingController {
    private final R2ClinicalApplicationService service;
    ResidentSchedulingController(R2ClinicalApplicationService service) { this.service = service; }
    /** 查询仍可预约且尚未开始的号源。 */
    @GetMapping("/slots") List<SlotView> slots() { return service.availableSlots(); }
    /** 查询当前居民全部预约。 */
    @GetMapping("/appointments") List<ScheduledAppointmentView> appointments(@AuthenticationPrincipal Jwt jwt) {
        return service.residentAppointments(claim(jwt, "patientId"));
    }

    /**
     * 为当前居民预约号源。
     *
     * <p>{@code Idempotency-Key} 用于安全重放：首次成功返回 201，相同请求重放返回 200。</p>
     */
    @PostMapping("/appointments") ResponseEntity<AppointmentView> book(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody BookRequest request) {
        BookingResult result = service.book(claim(jwt, "patientId"), key,
                new BookAppointmentCommand(request.slotId(), request.reason()));
        return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED).body(result.appointment());
    }
    /** 取消当前居民本人尚未进入接诊流程的预约。 */
    @DeleteMapping("/appointments/{id}") AppointmentView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return service.cancel(claim(jwt, "patientId"), id);
    }
    /** 居民预约请求。 */
    record BookRequest(@NotNull Long slotId, String reason) {}

    /** 从 JWT 中读取必需的数值型业务主体标识。 */
    static long claim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (value instanceof Number n) return n.longValue();
        throw new org.springframework.security.access.AccessDeniedException("账号未关联业务档案");
    }
}

/** 医护端排班建档、签到和发起接诊入口。 */
@RestController @RequestMapping("/api/v1/staff/scheduling")
class StaffSchedulingController {
    private final R2ClinicalApplicationService service;
    StaffSchedulingController(R2ClinicalApplicationService service) { this.service = service; }
    /** 查询当前岗位可办理的预约。 */
    @GetMapping("/appointments") List<ScheduledAppointmentView> appointments(@AuthenticationPrincipal Jwt jwt) {
        return service.staffAppointments(staff(jwt), hasRole(jwt, "DOCTOR"));
    }
    /** 查询已签到或接诊中的候诊队列。 */
    @GetMapping("/queue") List<QueueView> queue(@AuthenticationPrincipal Jwt jwt) {
        return service.staffQueue(staff(jwt), hasRole(jwt, "DOCTOR"));
    }
    /** 医生在有效任职站点和科室内创建未来排班及其号源。 */
    @PostMapping("/sessions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('DOCTOR')")
    SessionView open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SessionRequest r) {
        return service.openSession(staff(jwt), new SessionCommand(r.siteId(), r.departmentId(), r.startsAt(), r.endsAt(), r.slotMinutes()));
    }
    /** 护士为所属站点的预约办理签到并生成候诊序号。 */
    @PostMapping("/appointments/{id}/check-in") @PreAuthorize("hasRole('NURSE')")
    AppointmentView checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) { return service.checkIn(staff(jwt), id); }
    /** 排班医生接诊已签到患者并创建草稿病历。 */
    @PostMapping("/appointments/{id}/start") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('DOCTOR')")
    EncounterView start(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) { return service.startEncounter(staff(jwt), id); }
    /** 排班时段及号源粒度请求。 */
    record SessionRequest(@NotNull Long siteId, @NotNull Long departmentId, @NotNull LocalDateTime startsAt,
                          @NotNull LocalDateTime endsAt, int slotMinutes) {}
    /** 兼容新旧令牌声明名称并返回医护档案标识。 */
    static long staff(Jwt jwt) {
        Object value = jwt.getClaim("staffProfileId");
        return value instanceof Number n ? n.longValue() : ResidentSchedulingController.claim(jwt, "staffId");
    }
    private static boolean hasRole(Jwt jwt, String role) {
        Object roles = jwt.getClaim("roles");
        return roles instanceof List<?> values && values.contains(role);
    }
}

/** 医生端病历草稿、诊断和签署入口。 */
@RestController @RequestMapping("/api/v1/staff/encounters")
class StaffEncounterController {
    private final R2ClinicalApplicationService service;
    StaffEncounterController(R2ClinicalApplicationService service) { this.service = service; }
    /** 查询医生本人的接诊记录。 */
    @GetMapping @PreAuthorize("hasRole('DOCTOR')")
    List<EncounterView> encounters(@AuthenticationPrincipal Jwt jwt) {
        return service.staffEncounters(StaffSchedulingController.staff(jwt));
    }
    /** 保存本人接诊记录的 SOAP 草稿，并校验客户端乐观锁版本。 */
    @PutMapping("/{id}/draft") @PreAuthorize("hasRole('DOCTOR')")
    EncounterView draft(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody DraftRequest r) {
        return service.saveDraft(StaffSchedulingController.staff(jwt), id, new DraftCommand(r.body(), r.version()));
    }
    /** 向本人尚未签署的接诊记录添加结构化诊断。 */
    @PostMapping("/{id}/diagnoses") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('DOCTOR')")
    DiagnosisView diagnosis(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody DiagnosisRequest r) {
        return service.addDiagnosis(StaffSchedulingController.staff(jwt), id, new DiagnosisCommand(r.code(), r.name(), r.type()));
    }
    /** 签署病历并生成不可变文档版本；签署后原记录不可继续编辑。 */
    @PostMapping("/{id}/sign") @PreAuthorize("hasRole('DOCTOR')")
    EncounterView sign(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestBody SignRequest r) {
        return service.sign(StaffSchedulingController.staff(jwt), jwt.getSubject(), id, new SignCommand(r.version()));
    }
    /** 病历正文及客户端读取到的版本号。 */
    record DraftRequest(@NotBlank String body, long version) {}
    /** 结构化诊断请求。 */
    record DiagnosisRequest(@NotBlank String code, @NotBlank String name, @NotBlank String type) {}
    /** 病历签署请求。 */
    record SignRequest(long version) {}
}
