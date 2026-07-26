package cn.stylefeng.guns.modular.chronic_disease.service.impl;

import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.model.ChronicFollowup;
import cn.stylefeng.guns.modular.system.model.ChronicFollowupPlan;
import cn.stylefeng.guns.modular.system.dao.ChronicFollowupMapper;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicDiseaseService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupPlanService;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 慢病随访记录 Service 实现类
 */
@Service
public class ChronicFollowupServiceImpl extends ServiceImpl<ChronicFollowupMapper, ChronicFollowup>
        implements IChronicFollowupService {

    @Autowired
    private IChronicDiseaseService chronicDiseaseService;

    @Autowired
    private IChronicFollowupPlanService chronicFollowupPlanService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeFollowup(ChronicFollowup followup, Map<String, Object> clinicalData) {
        Map<String, Object> result = new HashMap<>();

        followup.setStatus(1);
        this.insert(followup);

        ChronicDisease chronic = chronicDiseaseService.selectById(followup.getChronicId());
        if (chronic == null) {
            result.put("code", 500);
            result.put("message", "档案不存在");
            return result;
        }

        // 若有临床数据则重新评估风险等级
        if (clinicalData != null && !clinicalData.isEmpty()) {
            String assessedRisk = chronicDiseaseService.assessRiskLevel(chronic.getDiseaseType(), clinicalData);
            followup.setRiskLevel(assessedRisk);
            this.updateById(followup);
            chronic.setRiskLevel(assessedRisk);
        } else if (followup.getRiskLevel() != null && !followup.getRiskLevel().isEmpty()) {
            chronic.setRiskLevel(followup.getRiskLevel());
        }

        chronic.setUpdateTime(new Date());
        chronicDiseaseService.updateById(chronic);

        // 标记最近一条待执行的随访计划为已执行
        EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
        planWrapper.eq("chronic_id", followup.getChronicId());
        planWrapper.eq("status", 0);
        planWrapper.orderBy("plan_date", true);
        List<ChronicFollowupPlan> pendingPlans = chronicFollowupPlanService.selectList(planWrapper);
        if (!pendingPlans.isEmpty()) {
            pendingPlans.get(0).setStatus(1);
            chronicFollowupPlanService.updateById(pendingPlans.get(0));
        }

        // 自动生成下次随访计划
        generateFollowupPlan(chronic);

        result.put("code", 200);
        result.put("message", "随访完成，已自动生成下次计划");
        result.put("followupId", followup.getId());
        result.put("assessedRisk", chronic.getRiskLevel());
        return result;
    }

    @Override
    public String reassessRisk(Integer chronicId, Map<String, Object> clinicalData) {
        ChronicDisease chronic = chronicDiseaseService.selectById(chronicId);
        if (chronic == null) {
            return null;
        }
        String newRisk = chronicDiseaseService.assessRiskLevel(chronic.getDiseaseType(), clinicalData);
        chronic.setRiskLevel(newRisk);
        chronic.setUpdateTime(new Date());
        chronicDiseaseService.updateById(chronic);
        return newRisk;
    }

    @Override
    public void generateFollowupPlan(ChronicDisease chronic) {
        if (chronic == null || chronic.getStatus() != 1) {
            return;
        }

        // 检查是否已有待执行的计划
        EntityWrapper<ChronicFollowupPlan> checkWrapper = new EntityWrapper<>();
        checkWrapper.eq("chronic_id", chronic.getId());
        checkWrapper.eq("status", 0);
        if (chronicFollowupPlanService.selectCount(checkWrapper) > 0) {
            return;
        }

        Date nextDate = chronicDiseaseService.calculateNextFollowupDate(chronic.getRiskLevel());

        ChronicFollowupPlan plan = new ChronicFollowupPlan();
        plan.setChronicId(chronic.getId());
        plan.setPatientIdcard(chronic.getPatientIdcard());
        plan.setPatientName(chronic.getPatientName());
        plan.setDiseaseType(chronic.getDiseaseType());
        plan.setPlanDate(nextDate);
        plan.setPlanType("门诊");
        plan.setStatus(0);
        plan.setDoctorName(chronic.getDoctorName());
        plan.setDoctorId(chronic.getDoctorId());
        plan.setCreateTime(new Date());
        chronicFollowupPlanService.insert(plan);
    }
}
