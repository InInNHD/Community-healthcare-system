package com.community.healthcare.notification;

import com.community.healthcare.referral.application.R5PlatformService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @GetMapping("/api/v1/resident/records/releases") List<Map<String,Object>> releases(@AuthenticationPrincipal Jwt jwt){return service.releases(patient(jwt));}
    @PostMapping("/api/v1/resident/messages") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> message(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MessageRequest r){return service.message(patient(jwt),r.subject(),r.body());}
    @GetMapping("/api/v1/resident/messages") List<Map<String,Object>> messages(@AuthenticationPrincipal Jwt jwt){return service.messages(patient(jwt));}
    @PostMapping("/api/v1/resident/feedback") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> feedback(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ServiceFeedbackRequest r){return service.serviceFeedback(patient(jwt),r.businessType(),r.businessId(),r.rating(),r.comments());}
    @GetMapping("/api/v1/resident/feedback") List<Map<String,Object>> feedback(@AuthenticationPrincipal Jwt jwt){return service.feedback(patient(jwt));}
    private static long patient(Jwt jwt){Object v=jwt.getClaim("patientId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");}
    /** 限定资料范围和使用目的的授权请求。 */
    record ReleaseRequest(Long referralId,@NotBlank String scopeCode,@NotBlank String purpose){}
    /** 健康咨询留言请求，不用于互联网诊疗。 */
    record MessageRequest(@NotBlank String subject,@NotBlank String body){}
    /** 通用服务评价请求。 */
    record ServiceFeedbackRequest(@NotBlank String businessType,@NotBlank String businessId,@Min(1)@Max(5) int rating,String comments){}
}

/** 医护端居民留言查询和非诊断性答复入口。 */
@RestController
@RequestMapping("/api/v1/staff/messages")
@PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
class R5StaffEngagementController {
    private final R5PlatformService service;
    R5StaffEngagementController(R5PlatformService service){this.service=service;}
    @GetMapping List<Map<String,Object>> messages(@AuthenticationPrincipal Jwt jwt){return service.staffMessages(staff(jwt));}
    @PostMapping("/{id}/replies") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> reply(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody ReplyRequest request){return service.replyMessage(staff(jwt),id,request.body());}
    private static long staff(Jwt jwt){Object v=jwt.getClaim("staffProfileId");if(!(v instanceof Number))v=jwt.getClaim("staffId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联人员档案");}
    record ReplyRequest(@NotBlank String body){}
}
