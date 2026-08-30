package cn.stylefeng.guns.modular.chronic_disease.controller;

import cn.stylefeng.guns.core.log.LogObjectHolder;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.core.shiro.ShiroUser;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicDiseaseService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupPlanService;
import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.model.ChronicFollowup;
import cn.stylefeng.guns.modular.system.model.ChronicFollowupPlan;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 慢病管理控制器
 * 参考南京"超能家医"模式：高血压、糖尿病、冠心病、脑卒中、慢阻肺、慢性肾病六大慢病管理
 * 支持风险分级（低/中/高）和自动化随访
 *
 * @author guns
 */
@Controller
@RequestMapping("/chronicDisease")
public class ChronicDiseaseController extends BaseController {

    private String PREFIX = "/chronic_disease/chronicDisease/";

    @Autowired
    private IChronicDiseaseService chronicDiseaseService;

    @Autowired
    private IChronicFollowupService chronicFollowupService;

    @Autowired
    private IChronicFollowupPlanService chronicFollowupPlanService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); return; }
                try { setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(text)); }
                catch (Exception e1) {
                    try { setValue(new SimpleDateFormat("yyyy-MM-dd").parse(text)); }
                    catch (Exception e2) { setValue(null); }
                }
            }
        });
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); }
                else { setValue(new BigDecimal(text)); }
            }
        });
        binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); }
                else { setValue(Integer.valueOf(text)); }
            }
        });
    }

    // ==================== 页面跳转 ====================

    /**
     * 跳转到慢病管理首页（含仪表盘）
     */
    @RequestMapping("")
    public String index(Model model) {
        ShiroUser shiroUser = ShiroKit.getUser();
        model.addAttribute("userName", shiroUser.getName());
        return PREFIX + "chronicDisease.html";
    }

    /**
     * 跳转到添加慢病档案页
     */
    @RequestMapping("/chronicDisease_add")
    public String chronicDiseaseAdd(Model model) {
        model.addAttribute("doctorName", ShiroKit.getUser().getName());
        return PREFIX + "chronicDisease_add.html";
    }

    /**
     * 跳转到编辑慢病档案页
     */
    @RequestMapping("/chronicDisease_update/{chronicDiseaseId}")
    public String chronicDiseaseUpdate(@PathVariable Integer chronicDiseaseId, Model model) {
        ChronicDisease item = chronicDiseaseService.selectById(chronicDiseaseId);
        model.addAttribute("item", item);
        LogObjectHolder.me().set(item);
        return PREFIX + "chronicDisease_edit.html";
    }

    // ==================== 慢病档案 CRUD ====================

    /**
     * 获取慢病档案列表（支持多条件筛选）
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(
            @RequestParam(required = false) String diseaseType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String doctorName) {
        EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
        if (diseaseType != null && !diseaseType.isEmpty()) {
            wrapper.eq("disease_type", diseaseType);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq("risk_level", riskLevel);
        }
        if (patientName != null && !patientName.isEmpty()) {
            wrapper.like("patient_name", patientName);
        }
        if (doctorName != null && !doctorName.isEmpty()) {
            wrapper.eq("doctor_name", doctorName);
        }
        wrapper.orderBy("risk_level", false);
        wrapper.orderBy("update_time", false);
        return chronicDiseaseService.selectList(wrapper);
    }

    /**
     * 新增慢病档案（自动生成首次随访计划）
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    public Object add(ChronicDisease chronicDisease) {
        try {
            if (chronicDisease.getPatientName() == null || chronicDisease.getPatientName().trim().isEmpty()) {
                return ResponseData.error("患者姓名不能为空");
            }
            if (chronicDisease.getDiseaseType() == null || chronicDisease.getDiseaseType().trim().isEmpty()) {
                return ResponseData.error("请选择慢病类型");
            }
            chronicDisease.setCreateTime(new Date());
            chronicDisease.setUpdateTime(new Date());
            if (chronicDisease.getStatus() == null) {
                chronicDisease.setStatus(1);
            }
            if (chronicDisease.getRiskLevel() == null || chronicDisease.getRiskLevel().isEmpty()) {
                chronicDisease.setRiskLevel("低风险");
            }
            chronicDiseaseService.insert(chronicDisease);
            // 自动生成首次随访计划
            chronicFollowupService.generateFollowupPlan(chronicDisease);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 修改慢病档案（风险等级变更时重新生成计划）
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update(ChronicDisease chronicDisease) {
        try {
            if (chronicDisease.getId() == null) {
                return ResponseData.error("缺少档案ID");
            }
            ChronicDisease existing = chronicDiseaseService.selectById(chronicDisease.getId());
            if (existing == null) {
                return ResponseData.error("档案不存在");
            }
            chronicDisease.setUpdateTime(new Date());
            chronicDiseaseService.updateById(chronicDisease);

            // 风险等级变更时，删除旧未执行计划，重新生成
            if (chronicDisease.getRiskLevel() != null
                    && !chronicDisease.getRiskLevel().equals(existing.getRiskLevel())) {
                EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
                planWrapper.eq("chronic_id", chronicDisease.getId());
                planWrapper.eq("status", 0);
                chronicFollowupPlanService.delete(planWrapper);
                ChronicDisease updated = chronicDiseaseService.selectById(chronicDisease.getId());
                chronicFollowupService.generateFollowupPlan(updated);
            }
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除慢病档案（同时删除关联的随访计划和记录）
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete(@RequestParam Integer chronicDiseaseId) {
        try {
            // 级联软删除：关联的随访计划
            EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
            planWrapper.eq("chronic_id", chronicDiseaseId);
            List<ChronicFollowupPlan> plans = chronicFollowupPlanService.selectList(planWrapper);
            for (ChronicFollowupPlan plan : plans) {
                plan.setIsDeleted(1);
                chronicFollowupPlanService.updateById(plan);
            }

            // 级联软删除：关联的随访记录
            EntityWrapper<ChronicFollowup> followupWrapper = new EntityWrapper<>();
            followupWrapper.eq("chronic_id", chronicDiseaseId);
            List<ChronicFollowup> followups = chronicFollowupService.selectList(followupWrapper);
            for (ChronicFollowup fw : followups) {
                fw.setIsDeleted(1);
                chronicFollowupService.updateById(fw);
            }

            // 软删除档案本身
            ChronicDisease entity = chronicDiseaseService.selectById(chronicDiseaseId);
            entity.setIsDeleted(1);
            chronicDiseaseService.updateById(entity);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 慢病档案详情
     */
    @RequestMapping(value = "/detail/{chronicDiseaseId}")
    @ResponseBody
    public Object detail(@PathVariable Integer chronicDiseaseId) {
        ChronicDisease item = chronicDiseaseService.selectById(chronicDiseaseId);
        if (item == null) {
            return ResponseData.error("档案不存在");
        }
        // 附加字典映射
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("patientName", item.getPatientName());
        result.put("patientIdcard", item.getPatientIdcard());
        result.put("diseaseType", item.getDiseaseType());
        result.put("riskLevel", item.getRiskLevel());
        result.put("diagnosisDate", item.getDiagnosisDate());
        result.put("doctorName", item.getDoctorName());
        result.put("status", item.getStatus());
        result.put("remark", item.getRemark());
        result.put("createTime", item.getCreateTime());
        result.put("updateTime", item.getUpdateTime());
        // 随访统计
        EntityWrapper<ChronicFollowup> fwWrapper = new EntityWrapper<>();
        fwWrapper.eq("chronic_id", item.getId());
        result.put("followupCount", chronicFollowupService.selectCount(fwWrapper));
        EntityWrapper<ChronicFollowupPlan> pnWrapper = new EntityWrapper<>();
        pnWrapper.eq("chronic_id", item.getId());
        pnWrapper.eq("status", 0);
        result.put("pendingPlanCount", chronicFollowupPlanService.selectCount(pnWrapper));
        return result;
    }

    // ==================== 风险评估 ====================

    /**
     * 根据临床指标自动评估风险等级
     */
    @RequestMapping(value = "/assessRisk")
    @ResponseBody
    public Object assessRisk(
            @RequestParam String diseaseType,
            @RequestParam(required = false) Integer systolic,
            @RequestParam(required = false) Integer diastolic,
            @RequestParam(required = false) Double bloodSugar,
            @RequestParam(required = false) Double hba1c,
            @RequestParam(required = false) Integer nyha,
            @RequestParam(required = false) Boolean acsHistory,
            @RequestParam(required = false) Integer nihss,
            @RequestParam(required = false) Double fev1,
            @RequestParam(required = false) Double egfr,
            @RequestParam(required = false) Double proteinuria) {
        try {
            Map<String, Object> clinicalData = new HashMap<>();
            if (systolic != null) clinicalData.put("systolic", systolic);
            if (diastolic != null) clinicalData.put("diastolic", diastolic);
            if (bloodSugar != null) clinicalData.put("bloodSugar", bloodSugar);
            if (hba1c != null) clinicalData.put("hba1c", hba1c);
            if (nyha != null) clinicalData.put("nyha", nyha);
            if (acsHistory != null) clinicalData.put("acsHistory", acsHistory);
            if (nihss != null) clinicalData.put("nihss", nihss);
            if (fev1 != null) clinicalData.put("fev1", fev1);
            if (egfr != null) clinicalData.put("egfr", egfr);
            if (proteinuria != null) clinicalData.put("proteinuria", proteinuria);

            String riskLevel = chronicDiseaseService.assessRiskLevel(diseaseType, clinicalData);
            int intervalDays = chronicDiseaseService.getFollowupIntervalDays(riskLevel);

            Map<String, Object> result = new HashMap<>();
            result.put("riskLevel", riskLevel);
            result.put("intervalDays", intervalDays);
            result.put("nextFollowupDate", chronicDiseaseService.calculateNextFollowupDate(riskLevel));
            return result;
        } catch (Exception e) {
            return ResponseData.error("评估失败：" + e.getMessage());
        }
    }

    // ==================== 随访记录管理 ====================

    /**
     * 获取随访记录列表
     */
    @RequestMapping(value = "/followup/list")
    @ResponseBody
    public Object followupList(@RequestParam(required = false) Integer chronicId) {
        EntityWrapper<ChronicFollowup> wrapper = new EntityWrapper<>();
        if (chronicId != null) {
            wrapper.eq("chronic_id", chronicId);
        }
        wrapper.orderBy("followup_date", false);
        return chronicFollowupService.selectList(wrapper);
    }

    /**
     * 执行随访（含自动风险评估和计划生成）
     */
    @RequestMapping(value = "/followup/add")
    @ResponseBody
    public Object addFollowup(ChronicFollowup followup,
            @RequestParam(required = false) Integer systolic,
            @RequestParam(required = false) Integer diastolic,
            @RequestParam(required = false) Double bloodSugar,
            @RequestParam(required = false) Double hba1c,
            @RequestParam(required = false) Integer nihss,
            @RequestParam(required = false) Double fev1,
            @RequestParam(required = false) Double egfr) {
        try {
            followup.setFollowupDoctor(ShiroKit.getUser().getName());

            Map<String, Object> clinicalData = new HashMap<>();
            if (systolic != null) clinicalData.put("systolic", systolic);
            if (diastolic != null) clinicalData.put("diastolic", diastolic);
            if (bloodSugar != null) clinicalData.put("bloodSugar", bloodSugar);
            if (hba1c != null) clinicalData.put("hba1c", hba1c);
            if (nihss != null) clinicalData.put("nihss", nihss);
            if (fev1 != null) clinicalData.put("fev1", fev1);
            if (egfr != null) clinicalData.put("egfr", egfr);

            Map<String, Object> result = chronicFollowupService.executeFollowup(followup, clinicalData);
            return result;
        } catch (Exception e) {
            return ResponseData.error("随访失败：" + e.getMessage());
        }
    }

    /**
     * 获取随访记录详情
     */
    @RequestMapping(value = "/followup/detail/{followupId}")
    @ResponseBody
    public Object followupDetail(@PathVariable Integer followupId) {
        return chronicFollowupService.selectById(followupId);
    }

    // ==================== 随访计划管理 ====================

    /**
     * 获取随访计划列表（自动标记逾期）
     */
    @RequestMapping(value = "/plan/list")
    @ResponseBody
    public Object planList(@RequestParam(required = false) Integer chronicId) {
        // 自动标记过期计划
        EntityWrapper<ChronicFollowupPlan> expiredWrapper = new EntityWrapper<>();
        expiredWrapper.eq("status", 0);
        expiredWrapper.lt("plan_date", new Date());
        List<ChronicFollowupPlan> expiredPlans = chronicFollowupPlanService.selectList(expiredWrapper);
        for (ChronicFollowupPlan plan : expiredPlans) {
            plan.setStatus(2);
            chronicFollowupPlanService.updateById(plan);
        }

        EntityWrapper<ChronicFollowupPlan> wrapper = new EntityWrapper<>();
        if (chronicId != null) {
            wrapper.eq("chronic_id", chronicId);
        }
        wrapper.orderBy("plan_date", true);
        return chronicFollowupPlanService.selectList(wrapper);
    }

    /**
     * 手动新增随访计划
     */
    @RequestMapping(value = "/plan/add")
    @ResponseBody
    public Object addPlan(ChronicFollowupPlan plan) {
        try {
            plan.setCreateTime(new Date());
            if (plan.getStatus() == null) {
                plan.setStatus(0);
            }
            chronicFollowupPlanService.insert(plan);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 删除随访计划
     */
    @RequestMapping(value = "/plan/delete")
    @ResponseBody
    public Object deletePlan(@RequestParam Integer planId) {
        ChronicFollowupPlan plan = chronicFollowupPlanService.selectById(planId);
        plan.setIsDeleted(1);
        chronicFollowupPlanService.updateById(plan);
        return SUCCESS_TIP;
    }

    // ==================== 统计数据 ====================

    /**
     * 获取慢病管理统计数据（仪表盘）
     */
    @RequestMapping(value = "/stats")
    @ResponseBody
    public Object stats() {
        return chronicDiseaseService.getStats(null);
    }

    /**
     * 获取指定病种的随访建议模板
     */
    @RequestMapping(value = "/followupTemplate")
    @ResponseBody
    public Object followupTemplate(@RequestParam String diseaseType) {
        try {
            Map<String, String> template = chronicDiseaseService.getFollowupTemplate(diseaseType);
            if (template.isEmpty()) {
                return ResponseData.error("未知的病种类型");
            }
            return template;
        } catch (Exception e) {
            return ResponseData.error("获取模板失败：" + e.getMessage());
        }
    }

    /**
     * 获取待执行随访计划提醒（按日期排序，前20条）
     */
    @RequestMapping(value = "/pendingReminders")
    @ResponseBody
    public Object pendingReminders() {
        EntityWrapper<ChronicFollowupPlan> wrapper = new EntityWrapper<>();
        wrapper.eq("status", 0);
        wrapper.orderBy("plan_date", true);
        wrapper.last("LIMIT 20");
        return chronicFollowupPlanService.selectList(wrapper);
    }
}
