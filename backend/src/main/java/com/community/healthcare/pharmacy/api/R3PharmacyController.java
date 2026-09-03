package com.community.healthcare.pharmacy.api;

import com.community.healthcare.pharmacy.infrastructure.R3PharmacyBillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 医生和药师共享的处方流转入口，端点权限按具体职责细分。 */
@RestController
@RequestMapping("/api/v1/staff/pharmacy")
public class R3PharmacyController {
    private final R3PharmacyBillingService service;
    public R3PharmacyController(R3PharmacyBillingService service) { this.service = service; }

    /** 返回当前医生或药师可见的处方队列。 */
    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR','PHARMACIST')")
    List<R3PharmacyBillingService.PrescriptionView> prescriptions(@AuthenticationPrincipal Jwt jwt) {
        return hasRole(jwt, "DOCTOR") ? service.doctorPrescriptions(staffId(jwt))
                : service.pharmacyPrescriptions(staffId(jwt));
    }

    /** 返回处方可选药品及可用库存。 */
    @GetMapping("/skus")
    @PreAuthorize("hasAnyRole('DOCTOR','PHARMACIST')")
    List<Map<String, Object>> skus() { return service.medicineSkus(); }

    /** 医生基于线下已签署接诊创建处方。 */
    @PostMapping("/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    R3PharmacyBillingService.PrescriptionView create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody R3PharmacyBillingService.PrescriptionCommand command) {
        return service.createPrescription(staffId(jwt), command);
    }

    @PostMapping("/prescriptions/{id}/sign")
    @PreAuthorize("hasRole('DOCTOR')")
    R3PharmacyBillingService.PrescriptionView sign(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        return service.sign(staffId(jwt), id, key, auth.getName());
    }

    /** 药师审方并记录通过或拒绝意见。 */
    @PostMapping("/prescriptions/{id}/review")
    @PreAuthorize("hasRole('PHARMACIST')")
    R3PharmacyBillingService.PrescriptionView review(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key,
            @RequestBody R3PharmacyBillingService.ReviewCommand command) {
        return service.review(staffId(jwt), id, key, command, auth.getName());
    }

    @PostMapping("/prescriptions/{id}/pick")
    @PreAuthorize("hasRole('PHARMACIST')")
    R3PharmacyBillingService.PrescriptionView pick(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return service.pick(staffId(jwt), id);
    }

    @PostMapping("/prescriptions/{id}/check")
    @PreAuthorize("hasRole('PHARMACIST')")
    R3PharmacyBillingService.PrescriptionView check(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return service.check(staffId(jwt), id);
    }

    /** 药师完成核对后幂等发药并原子扣减库存。 */
    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasRole('PHARMACIST')")
    R3PharmacyBillingService.PrescriptionView dispense(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        return service.dispense(staffId(jwt), id, key, auth.getName());
    }

    private long staffId(Jwt jwt) {
        Object value = jwt.getClaim("staffProfileId");
        if (!(value instanceof Number)) value = jwt.getClaim("staffId");
        if (!(value instanceof Number number)) throw new IllegalArgumentException("令牌缺少医护人员标识");
        return number.longValue();
    }

    private boolean hasRole(Jwt jwt, String role) {
        Object roles = jwt.getClaim("roles");
        return roles instanceof List<?> values && values.contains(role);
    }
}
