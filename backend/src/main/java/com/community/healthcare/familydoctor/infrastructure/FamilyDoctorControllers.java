package com.community.healthcare.familydoctor.infrastructure;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 管理端家庭医生团队、成员和服务包配置入口。 */
@RestController @RequestMapping("/api/v1/admin/family-doctor")
class FamilyDoctorAdminController{
 private final FamilyDoctorApplicationService s; FamilyDoctorAdminController(FamilyDoctorApplicationService s){this.s=s;}
 @GetMapping("/catalog") List<Map<String,Object>> catalog(){return s.adminCatalog();}
 @PostMapping("/teams") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> team(@RequestBody Team r){return s.createTeam(r.organizationId(),r.siteId(),r.code(),r.name());}
 @PostMapping("/teams/{id}/members") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> member(@PathVariable long id,@RequestBody Member r){return s.addMember(id,r.staffId(),r.role());}
 @PostMapping("/packages") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> pack(@RequestBody Pack r){return s.createPackage(r.organizationId(),r.code(),r.name(),r.versionNo(),r.effectiveFrom(),r.items());}
 /** 团队创建请求。 */
 record Team(long organizationId,long siteId,String code,String name){}
 /** 团队成员请求。 */
 record Member(long staffId,String role){}
 /** 带版本的服务包创建请求。 */
 record Pack(long organizationId,String code,String name,int versionNo,LocalDate effectiveFrom,List<FamilyDoctorApplicationService.PackageItem> items){}
}
/** 医护端签约创建和服务任务履约入口。 */
@RestController @RequestMapping("/api/v1/staff/family-doctor")
@PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
class FamilyDoctorStaffController{
 private final FamilyDoctorApplicationService s; FamilyDoctorStaffController(FamilyDoctorApplicationService s){this.s=s;}
 @GetMapping("/tasks") List<Map<String,Object>> tasks(@AuthenticationPrincipal Jwt j){return s.staffTasks(staff(j));}
 @PostMapping("/contracts") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> contract(@AuthenticationPrincipal Jwt j,@RequestHeader("Idempotency-Key")String k,@RequestBody Contract r){return s.createContract(staff(j),k,new FamilyDoctorApplicationService.ContractCommand(r.patientId(),r.teamId(),r.packageId(),r.startsOn(),r.endsOn()));}
 @PostMapping("/contracts/{id}/submit") Map<String,Object> submit(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.submit(staff(j),id,k);}
 @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> task(@AuthenticationPrincipal Jwt j,@RequestHeader("Idempotency-Key")String k,@RequestBody Task r){return s.createTask(staff(j),k,new FamilyDoctorApplicationService.TaskCommand(r.contractId(),r.taskType(),r.sourceType(),r.sourceId(),r.dueAt()));}
 @PostMapping("/tasks/{id}/assign") Map<String,Object> assign(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Assignee r){return s.assign(staff(j),id,r.staffId(),k);}
 @PostMapping("/tasks/{id}/start") Map<String,Object> start(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.start(staff(j),id,k);}
 @PostMapping("/tasks/{id}/complete") Map<String,Object> complete(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Summary r){return s.complete(staff(j),id,k,r.summary());}
 @PostMapping("/tasks/{id}/exception-close") Map<String,Object> close(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k,@RequestBody Summary r){return s.closeException(staff(j),id,k,r.summary());}
 static long staff(Jwt j){Object v=j.getClaim("staffProfileId");if(!(v instanceof Number))v=j.getClaim("staffId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联人员档案");}
 /** 家医签约创建请求。 */
 record Contract(long patientId,long teamId,long packageId,LocalDate startsOn,LocalDate endsOn){}
 /** 履约任务创建请求。 */
 record Task(long contractId,String taskType,String sourceType,Long sourceId,LocalDateTime dueAt){}
 /** 任务受派医护请求。 */
 record Assignee(long staffId){}
 /** 任务完成摘要或异常关闭原因。 */
 record Summary(String summary){}
}
/** 居民端本人签约查询和确认入口。 */
@RestController @RequestMapping("/api/v1/resident/family-doctor")
class FamilyDoctorResidentController{
 private final FamilyDoctorApplicationService s;FamilyDoctorResidentController(FamilyDoctorApplicationService s){this.s=s;}
 @GetMapping("/contracts") List<Map<String,Object>> contracts(@AuthenticationPrincipal Jwt j){return s.residentContracts(patient(j));}
 @PostMapping("/contracts/{id}/confirm") Map<String,Object> confirm(@AuthenticationPrincipal Jwt j,@PathVariable long id,@RequestHeader("Idempotency-Key")String k){return s.confirm(patient(j),id,k);}
 static long patient(Jwt j){Object v=j.getClaim("patientId");if(v instanceof Number n)return n.longValue();throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");}
}
