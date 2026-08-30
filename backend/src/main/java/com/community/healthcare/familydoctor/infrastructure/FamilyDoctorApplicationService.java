package com.community.healthcare.familydoctor.infrastructure;

import com.community.healthcare.familydoctor.domain.ContractStatus;
import com.community.healthcare.familydoctor.domain.ServiceTaskStatus;
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
 * 家庭医生团队、服务包、签约合同和履约任务的应用服务。
 *
 * <p>所有居民相关写操作都先校验团队数据范围；关键状态迁移使用幂等键、历史表和审计表
 * 形成可追溯的履约链路。</p>
 */
@Service
public class FamilyDoctorApplicationService {
    private final JdbcTemplate jdbc;
    private final StaffPatientScope scope;
    public FamilyDoctorApplicationService(JdbcTemplate jdbc,StaffPatientScope scope) { this.jdbc = jdbc; this.scope=scope; }

    /** 在指定机构和服务站下创建家庭医生团队。 */
    @Transactional public Map<String,Object> createTeam(long orgId,long siteId,String code,String name) {
        long id=insert("insert into care_team(organization_id,site_id,code,name,active,created_at,updated_at,version) values(?,?,?,?,true,current_timestamp,current_timestamp,0)",orgId,siteId,text(code),text(name));
        return row("select * from care_team where id=?",id);
    }
    /** 将医护档案按职责加入团队。 */
    @Transactional public Map<String,Object> addMember(long teamId,long staffId,String role) {
        long id=insert("insert into care_team_member(team_id,staff_profile_id,member_role,active,joined_at,version) values(?,?,?,true,current_timestamp,0)",teamId,staffId,text(role));
        return row("select * from care_team_member where id=?",id);
    }
    /** 创建带版本和生效日期的服务包及其项目快照。 */
    @Transactional public Map<String,Object> createPackage(long orgId,String code,String name,int version,LocalDate from,List<PackageItem> items) {
        if(items==null||items.isEmpty()) throw new IllegalArgumentException("服务包至少包含一个项目");
        long id=insert("insert into service_package(organization_id,code,name,version_no,active,effective_from,created_at,updated_at,version) values(?,?,?,?,true,?,current_timestamp,current_timestamp,0)",orgId,text(code),text(name),version,from);
        for(PackageItem i:items) jdbc.update("insert into service_package_item(package_id,item_code,item_name,target_population,frequency_rule,created_at) values(?,?,?,?,?,current_timestamp)",id,text(i.code()),text(i.name()),i.population(),text(i.frequency()));
        return row("select * from service_package where id=?",id);
    }
    /** 在团队权限范围内幂等创建签约草稿。 */
    @Transactional public Map<String,Object> createContract(long staffId,String key,ContractCommand c) {
        scope.requireTeam(staffId,c.teamId(),c.patientId());
        requireKey(key);
        List<Map<String,Object>> replay=jdbc.queryForList("select * from fd_contract where idempotency_key=?",key);
        if(!replay.isEmpty()) return replay.get(0);
        long id=insert("insert into fd_contract(patient_id,team_id,package_id,status,starts_on,ends_on,created_by_staff_id,idempotency_key,created_at,updated_at,version) values(?,?,?,'DRAFT',?,?,?,?,current_timestamp,current_timestamp,0)",c.patientId(),c.teamId(),c.packageId(),c.startsOn(),c.endsOn(),staffId,key);
        history("fd_contract_history","contract_id",id,null,"DRAFT","STAFF",staffId,null);
        return contract(id);
    }
    /** 医护提交签约草稿，等待居民确认。 */
    @Transactional public Map<String,Object> submit(long staffId,long id,String key) { requireContractScope(staffId,id); return contractTransition(staffId,"STAFF",id,key,"SUBMIT",ContractStatus::submit,false); }
    /** 居民确认本人签约，使合同进入履约状态。 */
    @Transactional public Map<String,Object> confirm(long patientId,long id,String key) {
        Map<String,Object> c=contract(id); if(num(c,"patient_id")!=patientId) throw new EntityNotFoundException("签约记录不存在");
        return contractTransition(patientId,"RESIDENT",id,key,"CONFIRM",ContractStatus::confirm,true);
    }
    /** 查询居民本人的签约记录。 */
    public List<Map<String,Object>> residentContracts(long patientId){ return jdbc.queryForList("select id,team_id,package_id,status,starts_on,ends_on,resident_confirmed_at,version from fd_contract where patient_id=? order by id desc",patientId); }

    /** 从生效合同生成可分派的履约任务。 */
    @Transactional public Map<String,Object> createTask(long actor,String key,TaskCommand c) {
        requireKey(key); List<Map<String,Object>> replay=jdbc.queryForList("select * from fd_service_task where idempotency_key=?",key); if(!replay.isEmpty())return replay.get(0);
        Map<String,Object> contract=contract(c.contractId()); scope.requireTeam(actor,num(contract,"team_id"),num(contract,"patient_id")); if(!"ACTIVE".equals(contract.get("status")))throw new IllegalStateException("仅生效合同可创建任务");
        long id=insert("insert into fd_service_task(contract_id,patient_id,team_id,task_type,source_type,source_id,due_at,status,idempotency_key,created_at,updated_at,version) values(?,?,?,?,?,?,?,'PENDING_ASSIGNMENT',?,current_timestamp,current_timestamp,0)",c.contractId(),num(contract,"patient_id"),num(contract,"team_id"),text(c.taskType()),text(c.sourceType()),c.sourceId(),c.dueAt(),key);
        history("fd_service_task_history","task_id",id,null,"PENDING_ASSIGNMENT","STAFF",0,null); return task(id);
    }
    @Transactional public Map<String,Object> assign(long actor,long id,long assignee,String key){ requireTaskScope(actor,id); return taskTransition(actor,id,key,"ASSIGN",s->s.assign(),assignee,null); }
    @Transactional public Map<String,Object> start(long actor,long id,String key){ requireTaskScope(actor,id); return taskTransition(actor,id,key,"START",s->s.start(),null,null); }
    /** 完成服务任务并仅写入一次履约结果。 */
    @Transactional public Map<String,Object> complete(long actor,long id,String key,String summary){
        requireTaskScope(actor,id);
        Map<String,Object> result=taskTransition(actor,id,key,"COMPLETE",ServiceTaskStatus::complete,null,null);
        jdbc.update("insert into fd_service_fulfillment(task_id,fulfilled_by_staff_id,summary,fulfilled_at) select ?,?,?,current_timestamp where not exists(select 1 from fd_service_fulfillment where task_id=?)",id,actor,text(summary),id); return result;
    }
    @Transactional public Map<String,Object> closeException(long actor,long id,String key,String reason){ requireTaskScope(actor,id); return taskTransition(actor,id,key,"CLOSE_EXCEPTION",ServiceTaskStatus::closeException,null,text(reason)); }

    private void requireContractScope(long staff,long id){Map<String,Object> c=contract(id);scope.requireTeam(staff,num(c,"team_id"),num(c,"patient_id"));}
    private void requireTaskScope(long staff,long id){Map<String,Object> t=task(id);scope.requireTeam(staff,num(t,"team_id"),num(t,"patient_id"));}

    /** 在同一事务内执行合同状态迁移、历史记录、审计和幂等留痕。 */
    private Map<String,Object> contractTransition(long actor,String actorType,long id,String key,String op,java.util.function.Function<ContractStatus,ContractStatus> f,boolean confirm){
        requireKey(key); if(replayed("FD_CONTRACT_"+op,String.valueOf(actor),key))return contract(id);
        Map<String,Object> row=contract(id); ContractStatus from=ContractStatus.valueOf(String.valueOf(row.get("status"))),to=f.apply(from);
        jdbc.update("update fd_contract set status=?,resident_confirmed_at=case when ? then current_timestamp else resident_confirmed_at end,updated_at=current_timestamp,version=version+1 where id=?",to.name(),confirm,id);
        history("fd_contract_history","contract_id",id,from.name(),to.name(),actorType,actor,null); audit(String.valueOf(actor),actorType,"FD_CONTRACT_"+op,"FD_CONTRACT",id);
        remember("FD_CONTRACT_"+op,String.valueOf(actor),key,id); return contract(id);
    }
    /** 在同一事务内执行任务状态迁移并记录操作者和异常原因。 */
    private Map<String,Object> taskTransition(long actor,long id,String key,String op,java.util.function.Function<ServiceTaskStatus,ServiceTaskStatus> f,Long assignee,String reason){
        requireKey(key); if(replayed("FD_TASK_"+op,String.valueOf(actor),key))return task(id);
        Map<String,Object> row=task(id); ServiceTaskStatus from=ServiceTaskStatus.valueOf(String.valueOf(row.get("status"))),to=f.apply(from);
        jdbc.update("update fd_service_task set status=?,assigned_staff_id=coalesce(?,assigned_staff_id),updated_at=current_timestamp,version=version+1 where id=?",to.name(),assignee,id);
        history("fd_service_task_history","task_id",id,from.name(),to.name(),"STAFF",actor,reason); if("CLOSE_EXCEPTION".equals(op))audit(String.valueOf(actor),"STAFF","FD_TASK_EXCEPTION_CLOSED","FD_SERVICE_TASK",id);
        remember("FD_TASK_"+op,String.valueOf(actor),key,id); return task(id);
    }
    private void history(String table,String fk,long id,String from,String to,String actorType,long actor,String reason){
        if(table.equals("fd_contract_history"))jdbc.update("insert into fd_contract_history(contract_id,from_status,to_status,actor_type,actor_id,reason,occurred_at) values(?,?,?,?,?,?,current_timestamp)",id,from,to,actorType,actor,reason);
        else jdbc.update("insert into fd_service_task_history(task_id,from_status,to_status,actor_staff_id,reason,occurred_at) values(?,?,?,?,?,current_timestamp)",id,from,to,actor==0?null:actor,reason);
    }
    private void audit(String actor,String role,String action,String type,long id){ jdbc.update("insert into audit_event(occurred_at,actor,actor_role,action,resource_type,resource_id,outcome,purpose,details_json,correlation_id) values(current_timestamp,?,?,?,?,?,'SUCCESS','workflow','{}',null)",actor,role,action,type,String.valueOf(id)); }
    private boolean replayed(String scope,String actor,String key){return jdbc.queryForObject("select count(*) from idempotency_record where operation_scope=? and actor_id=? and idempotency_key=?",Integer.class,scope,actor,key)>0;}
    private void remember(String scope,String actor,String key,long id){jdbc.update("insert into idempotency_record(operation_scope,actor_id,idempotency_key,request_hash,resource_id,response_json,created_at,expires_at) values(?,?,?,'r4',?,'{}',?,?)",scope,actor,key,String.valueOf(id),LocalDateTime.now(),LocalDateTime.now().plusDays(1));}
    private Map<String,Object> contract(long id){return row("select * from fd_contract where id=?",id);} private Map<String,Object> task(long id){return row("select * from fd_service_task where id=?",id);}
    private Map<String,Object> row(String sql,Object...args){List<Map<String,Object>> rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new EntityNotFoundException("资源不存在");return rows.get(0);}
    private long insert(String sql,Object...args){GeneratedKeyHolder k=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement p=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},k);return k.getKey().longValue();}
    private long num(Map<String,Object>m,String k){return ((Number)m.get(k)).longValue();} private String text(String s){if(s==null||s.isBlank())throw new IllegalArgumentException("必填字段不能为空");return s.trim();} private void requireKey(String k){if(k==null||k.isBlank())throw new IllegalArgumentException("缺少 Idempotency-Key");}
    /** 服务包项目快照。 */
    public record PackageItem(String code,String name,String population,String frequency){}
    /** 创建家庭医生签约的命令。 */
    public record ContractCommand(long patientId,long teamId,long packageId,LocalDate startsOn,LocalDate endsOn){}
    /** 创建履约任务的命令。 */
    public record TaskCommand(long contractId,String taskType,String sourceType,Long sourceId,LocalDateTime dueAt){}
}
