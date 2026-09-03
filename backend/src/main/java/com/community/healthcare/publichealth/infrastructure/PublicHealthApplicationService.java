package com.community.healthcare.publichealth.infrastructure;

import com.community.healthcare.observability.RequestCorrelationFilter;

import com.community.healthcare.publichealth.domain.FollowUpVisitStatus;
import com.community.healthcare.publichealth.domain.HealthAlertStatus;
import com.community.healthcare.publichealth.domain.PriorityPopulationType;
import com.community.healthcare.residentregistry.application.StaffPatientScope;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成人慢病与老年人公卫管理应用服务。
 *
 * <p>覆盖重点人群登记、随访计划、随访审核、规则评估和告警处置。规则结果始终标记为
 * 非诊断性提示，不能替代线下医生诊断。</p>
 */
@Service
public class PublicHealthApplicationService {
 private final JdbcTemplate jdbc;private final StaffPatientScope scope; public PublicHealthApplicationService(JdbcTemplate jdbc,StaffPatientScope scope){this.jdbc=jdbc;this.scope=scope;}
 /** 在团队管理范围内幂等登记重点人群。 */
 @Transactional public Map<String,Object> register(long staff,String key,long patient,long team,PriorityPopulationType type){
  scope.requireTeam(staff,team,patient);
  requireKey(key);List<Map<String,Object>> old=jdbc.queryForList("select * from ph_registry where idempotency_key=?",key);if(!old.isEmpty())return old.get(0);
  long id=insert("insert into ph_registry(patient_id,population_type,managing_team_id,status,enrolled_on,idempotency_key,created_by_staff_id,created_at,updated_at,version) values(?,? ,?,'ACTIVE',current_date,?,?,current_timestamp,current_timestamp,0)",patient,type.name(),team,key,staff);return registry(id);
 }
 /** 为有效登记创建周期性随访计划。 */
 @Transactional public Map<String,Object> createPlan(long staff,long registry,String code,int cadence,LocalDate due){
  Map<String,Object> reg=registry(registry);scope.requireTeam(staff,num(reg,"managing_team_id"),num(reg,"patient_id"));
  if(cadence<=0)throw new IllegalArgumentException("随访周期必须大于零");long id=insert("insert into ph_follow_up_plan(registry_id,plan_code,cadence_days,next_due_on,status,created_at,updated_at,version) values(?,?,?,?,'ACTIVE',current_timestamp,current_timestamp,0)",registry,text(code),cadence,due);return row("select * from ph_follow_up_plan where id=?",id);
 }
 /** 基于有效计划幂等创建随访草稿。 */
 @Transactional public Map<String,Object> createVisit(long staff,String key,long plan,String findings){
  requireKey(key);List<Map<String,Object>> old=jdbc.queryForList("select * from ph_follow_up_visit where idempotency_key=?",key);if(!old.isEmpty())return old.get(0);
  Map<String,Object> p=row("select p.id,r.id registry_id,r.patient_id from ph_follow_up_plan p join ph_registry r on r.id=p.registry_id where p.id=? and p.status='ACTIVE' and r.status='ACTIVE'",plan);
  Map<String,Object> reg=registry(num(p,"registry_id"));scope.requireTeam(staff,num(reg,"managing_team_id"),num(p,"patient_id"));
  long id=insert("insert into ph_follow_up_visit(plan_id,registry_id,patient_id,performed_by_staff_id,status,findings_json,idempotency_key,created_at,updated_at,version) values(?,?,?,?,'DRAFT',?,?,current_timestamp,current_timestamp,0)",plan,num(p,"registry_id"),num(p,"patient_id"),staff,text(findings),key);return visit(id);
 }
 @Transactional public Map<String,Object> submitVisit(long staff,long id,String key){return visitTransition(staff,id,key,"SUBMIT",FollowUpVisitStatus::submit,null);}
 @Transactional public Map<String,Object> verifyVisit(long staff,long id,String key){return visitTransition(staff,id,key,"VERIFY",FollowUpVisitStatus::verify,null);}
 @Transactional public Map<String,Object> returnVisit(long staff,long id,String key,String reason){return visitTransition(staff,id,key,"RETURN",FollowUpVisitStatus::returnForCorrection,text(reason));}
 /** 修正被审核退回的随访内容，保留再次提交的状态。 */
 @Transactional public Map<String,Object> correctVisit(long staff,long id,String findings){Map<String,Object> v=visit(id);requireVisitScope(staff,v);if(FollowUpVisitStatus.valueOf(String.valueOf(v.get("status")))!=FollowUpVisitStatus.RETURNED)throw new IllegalStateException("仅退回记录可修正");jdbc.update("update ph_follow_up_visit set findings_json=?,return_reason=null,updated_at=current_timestamp,version=version+1 where id=? and status='RETURNED'",text(findings),id);return visit(id);}
 /** 创建显式非诊断性的公卫规则版本。 */
 @Transactional public Map<String,Object> createRule(String code,int version,PriorityPopulationType type,String expression,String actions,LocalDate from){
  long id=insert("insert into ph_rule_version(rule_code,version_no,population_type,expression_json,action_json,diagnostic,active,effective_from,created_at) values(?,?,?,?,?,false,true,?,current_timestamp)",text(code),version,type.name(),text(expression),text(actions),from);return row("select * from ph_rule_version where id=?",id);
 }
 /** 按人群类型运行规则并同时生成风险评估和待处置告警。 */
 @Transactional public Map<String,Object> evaluate(long staff,String key,long registryId,long ruleId,String severity,String message){
  requireKey(key);List<Map<String,Object>> old=jdbc.queryForList("select * from ph_health_alert where idempotency_key=?",key);if(!old.isEmpty())return old.get(0);
  Map<String,Object> registry=registry(registryId),rule=row("select * from ph_rule_version where id=? and active=true and diagnostic=false",ruleId);
  scope.requireTeam(staff,num(registry,"managing_team_id"),num(registry,"patient_id"));
  if(!String.valueOf(registry.get("population_type")).equals(String.valueOf(rule.get("population_type"))))throw new IllegalArgumentException("规则与重点人群类型不匹配");
  insert("insert into ph_risk_assessment(registry_id,rule_version_id,risk_level,evidence_json,diagnostic,evaluated_by_staff_id,evaluated_at) values(?,?,?,'{}',false,?,current_timestamp)",registryId,ruleId,text(severity),staff);
  long alert=insert("insert into ph_health_alert(registry_id,patient_id,rule_version_id,severity,status,message,non_diagnostic,idempotency_key,created_at,updated_at,version) values(?,?,?,?,'OPEN',?,true,?,current_timestamp,current_timestamp,0)",registryId,num(registry,"patient_id"),ruleId,text(severity),text(message),key);
  audit(String.valueOf(staff),"STAFF","PUBLIC_HEALTH_RULE_ALERT_CREATED","HEALTH_ALERT",alert);return alert(alert);
 }
 @Transactional public Map<String,Object> acknowledge(long staff,long id,String key){return alertTransition(staff,id,key,"ACKNOWLEDGE",HealthAlertStatus::acknowledge,null);}
 @Transactional public Map<String,Object> resolve(long staff,long id,String key,String note){return alertTransition(staff,id,key,"RESOLVE",HealthAlertStatus::resolve,text(note));}
 @Transactional public Map<String,Object> dismiss(long staff,long id,String key,String note){return alertTransition(staff,id,key,"DISMISS",HealthAlertStatus::dismiss,text(note));}
 /** 查询居民本人可见的随访记录。 */
 public List<Map<String,Object>> residentVisits(long patient){return jdbc.queryForList("select id,registry_id,status,findings_json,submitted_at,verified_at,return_reason from ph_follow_up_visit where patient_id=? order by id desc",patient);}
 /** 返回当前工作人员范围内的随访记录。 */
 public List<Map<String,Object>> staffVisits(long staff){return jdbc.queryForList("select v.id,v.patient_id,p.name as patient_name,v.status,v.findings_json,v.submitted_at,v.verified_at,v.return_reason from ph_follow_up_visit v join patient p on p.id=v.patient_id where v.performed_by_staff_id=? or exists(select 1 from patient_site_enrollment pe join staff_site_assignment sa on sa.site_id=pe.site_id where pe.patient_id=v.patient_id and pe.active=true and sa.staff_profile_id=? and sa.active=true and sa.valid_from<=current_timestamp and (sa.valid_to is null or sa.valid_to>current_timestamp)) order by v.id desc limit 100",staff,staff);}

 /** 幂等推进随访状态；审核通过后滚动计算下一次应随访日期。 */
 private Map<String,Object> visitTransition(long staff,long id,String key,String op,java.util.function.Function<FollowUpVisitStatus,FollowUpVisitStatus> fn,String reason){
  requireKey(key);if(replayed("PH_VISIT_"+op,String.valueOf(staff),key))return visit(id);Map<String,Object> r=visit(id);requireVisitScope(staff,r);FollowUpVisitStatus from=FollowUpVisitStatus.valueOf(String.valueOf(r.get("status"))),to=fn.apply(from);
  jdbc.update("update ph_follow_up_visit set status=?,submitted_at=case when ? then current_timestamp else submitted_at end,verified_by_staff_id=case when ? then ? else verified_by_staff_id end,verified_at=case when ? then current_timestamp else verified_at end,return_reason=?,updated_at=current_timestamp,version=version+1 where id=?",to.name(),"SUBMIT".equals(op),"VERIFY".equals(op),staff,"VERIFY".equals(op),reason,id);
  if("VERIFY".equals(op)){audit(String.valueOf(staff),"STAFF","PUBLIC_HEALTH_VISIT_VERIFIED","FOLLOW_UP_VISIT",id);Map<String,Object> p=row("select next_due_on,cadence_days from ph_follow_up_plan where id=?",num(r,"plan_id"));LocalDate due=((java.sql.Date)p.get("next_due_on")).toLocalDate().plusDays(((Number)p.get("cadence_days")).longValue());jdbc.update("update ph_follow_up_plan set next_due_on=?,updated_at=current_timestamp,version=version+1 where id=?",due,num(r,"plan_id"));}
  remember("PH_VISIT_"+op,String.valueOf(staff),key,id);return visit(id);
 }
 /** 在居民数据权限范围内幂等确认、解决或驳回健康告警。 */
 private Map<String,Object> alertTransition(long staff,long id,String key,String op,java.util.function.Function<HealthAlertStatus,HealthAlertStatus> fn,String note){
  requireKey(key);if(replayed("PH_ALERT_"+op,String.valueOf(staff),key))return alert(id);Map<String,Object> r=alert(id);scope.require(staff,num(r,"patient_id"));HealthAlertStatus to=fn.apply(HealthAlertStatus.valueOf(String.valueOf(r.get("status"))));boolean ack="ACKNOWLEDGE".equals(op);
  jdbc.update("update ph_health_alert set status=?,acknowledged_by_staff_id=case when ? then ? else acknowledged_by_staff_id end,acknowledged_at=case when ? then current_timestamp else acknowledged_at end,closed_by_staff_id=case when ? then ? else closed_by_staff_id end,closed_at=case when ? then current_timestamp else closed_at end,closure_note=?,updated_at=current_timestamp,version=version+1 where id=?",to.name(),ack,staff,ack,!ack,staff,!ack,note,id);
  if(!ack)audit(String.valueOf(staff),"STAFF","PUBLIC_HEALTH_ALERT_CLOSED","HEALTH_ALERT",id);remember("PH_ALERT_"+op,String.valueOf(staff),key,id);return alert(id);
 }
 private void audit(String actor,String role,String action,String type,long id){jdbc.update("insert into audit_event(occurred_at,actor,actor_role,action,resource_type,resource_id,outcome,purpose,details_json,correlation_id) values(current_timestamp,?,?,?,?,?,'SUCCESS','public-health','{\"diagnostic\":false}',?)",actor,role,action,type,String.valueOf(id), RequestCorrelationFilter.current());}
 private boolean replayed(String scope,String actor,String key){return jdbc.queryForObject("select count(*) from idempotency_record where operation_scope=? and actor_id=? and idempotency_key=?",Integer.class,scope,actor,key)>0;}
 private void remember(String scope,String actor,String key,long id){jdbc.update("insert into idempotency_record(operation_scope,actor_id,idempotency_key,request_hash,resource_id,response_json,created_at,expires_at) values(?,?,?,'r4',?,'{}',?,?)",scope,actor,key,String.valueOf(id),LocalDateTime.now(),LocalDateTime.now().plusDays(1));}
 private Map<String,Object> registry(long id){return row("select * from ph_registry where id=?",id);}private Map<String,Object> visit(long id){return row("select * from ph_follow_up_visit where id=?",id);}private Map<String,Object>alert(long id){return row("select * from ph_health_alert where id=?",id);}
 private void requireVisitScope(long staff,Map<String,Object> visit){Map<String,Object> reg=registry(num(visit,"registry_id"));scope.requireTeam(staff,num(reg,"managing_team_id"),num(visit,"patient_id"));}
 private Map<String,Object>row(String sql,Object...a){List<Map<String,Object>>r=jdbc.queryForList(sql,a);if(r.isEmpty())throw new EntityNotFoundException("资源不存在");return r.get(0);}private long insert(String sql,Object...a){GeneratedKeyHolder k=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement p=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);return p;},k);return k.getKey().longValue();}private long num(Map<String,Object>m,String k){return ((Number)m.get(k)).longValue();}private String text(String s){if(s==null||s.isBlank())throw new IllegalArgumentException("必填字段不能为空");return s.trim();}private void requireKey(String k){if(k==null||k.isBlank())throw new IllegalArgumentException("缺少 Idempotency-Key");}
}
