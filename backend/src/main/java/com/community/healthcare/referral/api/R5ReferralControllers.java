package com.community.healthcare.referral.api;

import com.community.healthcare.referral.application.R5PlatformService;
import com.community.healthcare.referral.domain.ReferralStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 医生端转诊创建、提交、回执登记和查询入口。 */
@RestController
@RequestMapping("/api/v1/staff/referrals")
@PreAuthorize("hasRole('DOCTOR')")
class StaffReferralController {
    private final R5PlatformService service;
    StaffReferralController(R5PlatformService service){this.service=service;}
    @GetMapping List<R5PlatformService.ReferralView> list(@AuthenticationPrincipal Jwt jwt){return service.staffReferrals(staff(jwt));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    R5PlatformService.ReferralView create(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody CreateRequest r){return service.create(staff(jwt),key,new R5PlatformService.CreateReferral(r.patientId(),r.encounterId(),r.targetOrganization(),r.targetDepartment(),r.reason()));}
    @PostMapping("/{id}/submit") R5PlatformService.ReferralView submit(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@RequestHeader("Idempotency-Key")String key){return service.submit(staff(jwt),id,key);}
    @PostMapping("/{id}/receipts") R5PlatformService.ReferralView receipt(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody ReceiptRequest r){return service.receipt(staff(jwt),id,ReferralStatus.valueOf(r.status()),r.note());}
    @GetMapping("/{id}") R5PlatformService.ReferralView get(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){return service.getForStaff(id,staff(jwt));}
    static long staff(Jwt jwt){Object v=jwt.getClaim("staffProfileId");if(!(v instanceof Number))v=jwt.getClaim("staffId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联医护档案");}
    /** 医护发起转诊请求。 */
    record CreateRequest(@NotNull Long patientId,Long encounterId,@NotBlank String targetOrganization,@NotBlank String targetDepartment,@NotBlank String reason){}
    /** 上级机构回执登记请求。 */
    record ReceiptRequest(@NotBlank String status,String note){}
}

/** 居民端本人转诊查询、知情同意和服务反馈入口。 */
@RestController
@RequestMapping("/api/v1/resident/referrals")
class ResidentReferralController {
    private final R5PlatformService service;
    ResidentReferralController(R5PlatformService service){this.service=service;}
    @GetMapping List<R5PlatformService.ReferralView> list(@AuthenticationPrincipal Jwt jwt){return service.residentReferrals(patient(jwt));}
    @GetMapping("/{id}") R5PlatformService.ReferralView get(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){return service.getOwned(id,patient(jwt));}
    @PostMapping("/{id}/consent") R5PlatformService.ReferralView consent(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){return service.consent(patient(jwt),id);}
    @PostMapping("/{id}/feedback") @ResponseStatus(HttpStatus.CREATED) R5PlatformService.ReferralView feedback(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody FeedbackRequest r){return service.referralFeedback(patient(jwt),id,r.rating(),r.comments());}
    static long patient(Jwt jwt){Object v=jwt.getClaim("patientId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");}
    /** 居民转诊服务评分请求。 */
    record FeedbackRequest(@Min(1)@Max(5) int rating,String comments){}
}
