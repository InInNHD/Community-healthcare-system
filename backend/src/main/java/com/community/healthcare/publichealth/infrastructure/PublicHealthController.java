package com.community.healthcare.publichealth.infrastructure;

import com.community.healthcare.publichealth.domain.PriorityPopulationType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 医护端重点人群、随访和风险告警工作台入口。 */
@RestController @RequestMapping("/api/v1/staff/public-health")
@PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
class PublicHealthStaffController{
 private final PublicHealthApplicationService s;PublicHealthStaffController(PublicHealthApplicationService s){this.s=s;}
 @PostMapping("/registries") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> registry(@AuthenticationPrincipal Jwt j,@RequestHeader("Idempotency-Key")String k,@RequestBody Registry r){return s.register(staff(j),k,r.patientId(),r.teamId(),r.populationType());}
 @PostMapping("/registries/{id}/plans") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> plan(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestBody Plan r){return s.createPlan(staff(j),id,r.code(),r.cadenceDays(),r.nextDueOn());}
 @PostMapping("/visits") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> visit(@AuthenticationPrincipal Jwt j,@RequestHeader("Idempotency-Key")String k,@RequestBody Visit r){return s.createVisit(staff(j),k,r.planId(),r.findingsJson());}
 @PostMapping("/visits/{id}/submit") Map<String,Object> submit(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.submitVisit(staff(j),id,k);}
 @PutMapping("/visits/{id}") Map<String,Object> correct(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestBody VisitCorrection r){return s.correctVisit(staff(j),id,r.findingsJson());}
 @PostMapping("/visits/{id}/verify") Map<String,Object> verify(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.verifyVisit(staff(j),id,k);}
 @PostMapping("/visits/{id}/return") Map<String,Object> ret(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Note n){return s.returnVisit(staff(j),id,k,n.note());}
 @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> rule(@RequestBody Rule r){return s.createRule(r.code(),r.versionNo(),r.populationType(),r.expressionJson(),r.actionJson(),r.effectiveFrom());}
 @PostMapping("/registries/{id}/evaluate") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> evaluate(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Evaluation r){return s.evaluate(staff(j),k,id,r.ruleVersionId(),r.severity(),r.message());}
 @PostMapping("/alerts/{id}/acknowledge") Map<String,Object> ack(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.acknowledge(staff(j),id,k);}
 @PostMapping("/alerts/{id}/resolve") Map<String,Object> resolve(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Note n){return s.resolve(staff(j),id,k,n.note());}
 @PostMapping("/alerts/{id}/dismiss") Map<String,Object> dismiss(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Note n){return s.dismiss(staff(j),id,k,n.note());}
 static long staff(Jwt j){Object v=j.getClaim("staffProfileId");if(!(v instanceof Number))v=j.getClaim("staffId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联人员档案");}
 /** 重点人群登记请求。 */
 record Registry(long patientId,long teamId,PriorityPopulationType populationType){}
 /** 周期随访计划请求。 */
 record Plan(String code,int cadenceDays,LocalDate nextDueOn){}
 /** 随访记录草稿请求。 */
 record Visit(long planId,String findingsJson){}
 /** 退回随访的修正请求。 */
 record VisitCorrection(String findingsJson){}
 /** 非诊断性公卫规则版本请求。 */
 record Rule(String code,int versionNo,PriorityPopulationType populationType,String expressionJson,String actionJson,LocalDate effectiveFrom){}
 /** 风险规则评估请求。 */
 record Evaluation(long ruleVersionId,String severity,String message){}
 /** 审核或告警处置说明。 */
 record Note(String note){}
}
/** 居民端本人随访记录查询入口。 */
@RestController @RequestMapping("/api/v1/resident/public-health")
class PublicHealthResidentController{
 private final PublicHealthApplicationService s;PublicHealthResidentController(PublicHealthApplicationService s){this.s=s;}
 @GetMapping("/visits") List<Map<String,Object>> visits(@AuthenticationPrincipal Jwt j){Object v=j.getClaim("patientId");if(v instanceof Number n)return s.residentVisits(n.longValue());throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");}
}
