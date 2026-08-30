package com.community.healthcare.notification;

import com.community.healthcare.referral.application.R5PlatformService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 居民端资料授权、健康咨询留言和服务反馈入口。 */
@RestController
class R5ResidentEngagementController {
    private final R5PlatformService service;
    R5ResidentEngagementController(R5PlatformService service){this.service=service;}
    @PostMapping("/api/v1/resident/records/releases") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> release(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ReleaseRequest r){return service.release(patient(jwt),r.referralId(),r.scopeCode(),r.purpose());}
    @PostMapping("/api/v1/resident/messages") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> message(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MessageRequest r){return service.message(patient(jwt),r.subject(),r.body());}
    @GetMapping("/api/v1/resident/messages") List<Map<String,Object>> messages(@AuthenticationPrincipal Jwt jwt){return service.messages(patient(jwt));}
    @PostMapping("/api/v1/resident/feedback") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> feedback(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ServiceFeedbackRequest r){return service.serviceFeedback(patient(jwt),r.businessType(),r.businessId(),r.rating(),r.comments());}
    private static long patient(Jwt jwt){Object v=jwt.getClaim("patientId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");}
    /** 限定资料范围和使用目的的授权请求。 */
    record ReleaseRequest(Long referralId,@NotBlank String scopeCode,@NotBlank String purpose){}
    /** 健康咨询留言请求，不用于互联网诊疗。 */
    record MessageRequest(@NotBlank String subject,@NotBlank String body){}
    /** 通用服务评价请求。 */
    record ServiceFeedbackRequest(@NotBlank String businessType,@NotBlank String businessId,@Min(1)@Max(5) int rating,String comments){}
}
