package com.community.healthcare.residentregistry.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 对需要立即拒绝的写用例执行工作人员—居民服务范围校验。
 *
 * <p>工作人员可因有效站点派驻或有效家庭医生团队成员身份获得范围；团队操作还要求团队、工作人员和居民
 * 最终落在同一服务站点。查询失败按最小权限原则拒绝。</p>
 */
@Component
public class StaffPatientScope {
    private final JdbcTemplate jdbc;
    public StaffPatientScope(JdbcTemplate jdbc){this.jdbc=jdbc;}

    /** 要求工作人员在居民当前有效服务站点内具有派驻或团队关系。 */
    public void require(long staffId,long patientId){
        Integer count=jdbc.queryForObject("select count(*) from patient_site_enrollment pe where pe.patient_id=? and pe.active=true and (exists(select 1 from staff_site_assignment sa where sa.staff_profile_id=? and sa.site_id=pe.site_id and sa.active=true and (sa.valid_to is null or sa.valid_to>current_timestamp)) or exists(select 1 from care_team_member tm join care_team t on t.id=tm.team_id where tm.staff_profile_id=? and tm.active=true and t.site_id=pe.site_id and t.active=true))",Integer.class,patientId,staffId,staffId);
        if(count==null||count==0)throw new AccessDeniedException("该居民不在当前工作人员的服务范围内");
    }

    /** 要求工作人员是指定团队成员，且团队站点覆盖该居民。 */
    public void requireTeam(long staffId,long teamId,long patientId){
        Integer count=jdbc.queryForObject("select count(*) from care_team_member tm join care_team t on t.id=tm.team_id join patient_site_enrollment pe on pe.site_id=t.site_id and pe.patient_id=? and pe.active=true where tm.team_id=? and tm.staff_profile_id=? and tm.active=true and t.active=true",Integer.class,patientId,teamId,staffId);
        if(count==null||count==0)throw new AccessDeniedException("工作人员、团队与居民服务站点范围不匹配");
    }
}
