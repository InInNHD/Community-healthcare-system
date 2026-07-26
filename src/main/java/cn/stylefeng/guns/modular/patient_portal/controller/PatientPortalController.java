package cn.stylefeng.guns.modular.patient_portal.controller;

import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.core.shiro.ShiroUser;
import cn.stylefeng.guns.modular.doctor_point.service.IDoctorPointService;
import cn.stylefeng.guns.modular.doctor_info.service.IDoctorInfoService;
import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineInfoService;
import cn.stylefeng.guns.modular.pateint_health_manager.service.IPatientHealthService;
import cn.stylefeng.guns.modular.patient.service.IPatientInfoService;
import cn.stylefeng.guns.modular.patient_history_manager.service.IPatientHistoryService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicDiseaseService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupPlanService;
import cn.stylefeng.guns.modular.system.dao.VaccinationRecordMapper;
import cn.stylefeng.guns.modular.system.dao.MaternalRecordMapper;
import cn.stylefeng.guns.modular.system.dao.ElderlyCheckupMapper;
import cn.stylefeng.guns.modular.system.dao.InfectiousDiseaseReportMapper;
import cn.stylefeng.guns.modular.system.model.DoctorInfo;
import cn.stylefeng.guns.modular.system.model.DoctorPoint;
import cn.stylefeng.guns.modular.system.model.MedicineInfo;
import cn.stylefeng.guns.modular.system.model.PatientHealth;
import cn.stylefeng.guns.modular.system.model.PatientHistory;
import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.model.ChronicFollowup;
import cn.stylefeng.guns.modular.system.model.ChronicFollowupPlan;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 居民端门户控制器
 *
 * @author guns
 */
@Controller
public class PatientPortalController extends BaseController {

    @Autowired
    private IPatientHealthService patientHealthService;

    @Autowired
    private IDoctorPointService doctorPointService;

    @Autowired
    private IPatientHistoryService patientHistoryService;

    @Autowired
    private IMedicineInfoService medicineInfoService;

    @Autowired
    private IPatientInfoService patientInfoService;

    @Autowired
    private IDoctorInfoService doctorInfoService;

    @Autowired
    private IChronicDiseaseService chronicDiseaseService;

    @Autowired
    private IChronicFollowupService chronicFollowupService;

    @Autowired
    private IChronicFollowupPlanService chronicFollowupPlanService;

    @Autowired private VaccinationRecordMapper vaccinationRecordMapper;
    @Autowired private MaternalRecordMapper maternalRecordMapper;
    @Autowired private ElderlyCheckupMapper elderlyCheckupMapper;
    @Autowired private InfectiousDiseaseReportMapper infectiousDiseaseReportMapper;

/**
     * 通过当前登录用户的user_id关联patient_info，获取居民身份证号
     */
    private String getMyPatientIdcard() {
        Integer userId = ShiroKit.getUser().getId();
        EntityWrapper<cn.stylefeng.guns.modular.system.model.PatientInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        List<cn.stylefeng.guns.modular.system.model.PatientInfo> list = patientInfoService.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0).getPaientIdcard();
    }

    /**
     * 跳转到居民端门户首页
     */
    @RequestMapping(value = "/patient_portal", method = RequestMethod.GET)
    public String index(Model model) {
        ShiroUser shiroUser = ShiroKit.getUser();
        model.addAttribute("userName", shiroUser.getName());
        model.addAttribute("userRole", "社区居民");

        String myIdcard = getMyPatientIdcard();

        //统计概览数据
        EntityWrapper<PatientHealth> healthWrapper = new EntityWrapper<>();
        healthWrapper.eq("patient_idcard", myIdcard);
        int myHealthCount = patientHealthService.selectCount(healthWrapper);

        EntityWrapper<DoctorPoint> appointmentWrapper = new EntityWrapper<>();
        appointmentWrapper.eq("patient_idcard", myIdcard);
        int myAppointmentCount = doctorPointService.selectCount(appointmentWrapper);

        EntityWrapper<PatientHistory> historyWrapper = new EntityWrapper<>();
        historyWrapper.eq("patient_idcard", myIdcard);
        int myHistoryCount = patientHistoryService.selectCount(historyWrapper);

        EntityWrapper<MedicineInfo> medicineWrapper = new EntityWrapper<>();
        int medicineCount = medicineInfoService.selectCount(medicineWrapper);

        model.addAttribute("myHealthCount", myHealthCount);
        model.addAttribute("myAppointmentCount", myAppointmentCount);
        model.addAttribute("myHistoryCount", myHistoryCount);
        model.addAttribute("medicineCount", medicineCount);

        return "/patient_portal.html";
    }

    /**
     * 获取我的健康记录
     */
    @RequestMapping(value = "/patient_portal/my_health", method = RequestMethod.POST)
    @ResponseBody
    public Object myHealth() {
        EntityWrapper<PatientHealth> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        wrapper.orderBy("date", false);
        return patientHealthService.selectList(wrapper);
    }

    /**
     * 获取个性化健康贴士（根据最新健康指标分析）
     */
    @RequestMapping(value = "/patient_portal/health_tips", method = RequestMethod.POST)
    @ResponseBody
    public Object healthTips() {
        try {
            String myIdcard = getMyPatientIdcard();
            if (myIdcard == null || myIdcard.isEmpty()) {
                return getGeneralTips();
            }
            EntityWrapper<PatientHealth> wrapper = new EntityWrapper<>();
            wrapper.eq("patient_idcard", myIdcard);
            wrapper.orderBy("date", false);
            List<PatientHealth> list = patientHealthService.selectList(wrapper);
            if (list == null || list.isEmpty()) {
                return getGeneralTips();
            }
            return getPersonalizedTips(list.get(0));
        } catch (Exception e) {
            return getGeneralTips();
        }
    }

    private List<Map<String, Object>> getGeneralTips() {
        List<Map<String, Object>> tips = new java.util.ArrayList<>();
        addTip(tips, "fa-heart", "#e3f2fd", "#1565c0", "定期体检", "建议每年至少进行一次全面体检，及早发现潜在健康问题。");
        addTip(tips, "fa-apple", "#e8f5e9", "#2e7d32", "合理饮食", "多食蔬菜水果，少油少盐少糖，保持营养均衡。");
        addTip(tips, "fa-walking", "#fff3e0", "#e65100", "适量运动", "每天坚持30分钟中等强度运动，如散步、太极拳等。");
        addTip(tips, "fa-bed", "#fce4ec", "#c62828", "充足睡眠", "保证每天7-8小时睡眠，避免熬夜，规律作息。");
        addTip(tips, "fa-smile-o", "#f3e5f5", "#7b1fa2", "心理健康", "保持积极心态，多与家人朋友交流，及时排解压力。");
        addTip(tips, "fa-tint", "#e0f7fa", "#00838f", "多饮水", "每天饮水1500-2000毫升，促进新陈代谢。");
        return tips;
    }

    private List<Map<String, Object>> getPersonalizedTips(PatientHealth h) {
        List<Map<String, Object>> tips = new java.util.ArrayList<>();
        int hr = h.getHeartJump() != null ? h.getHeartJump() : 0;
        int bp = h.getBloodPressure() != null ? h.getBloodPressure() : 0;
        int ox = h.getBloodOx() != null ? h.getBloodOx() : 0;

        if (bp >= 140) {
            addTip(tips, "fa-heartbeat", "#ffebee", "#c62828", "血压偏高（" + bp + "mmHg）",
                "您的收缩压已超过140mmHg，建议低盐饮食（<5g/天），避免饮酒，定期监测血压。如持续偏高请及时就医。");
            addTip(tips, "fa-cutlery", "#fff3e0", "#e65100", "低钠饮食",
                "减少加工食品摄入，用香草和香料代替盐调味，多吃富含钾的食物如香蕉、土豆。");
        } else if (bp >= 130) {
            addTip(tips, "fa-heartbeat", "#fff3e0", "#e65100", "血压临界偏高（" + bp + "mmHg）",
                "您的血压处于正常高值范围，注意减少盐摄入，增加有氧运动，保持健康体重。");
        } else if (bp < 90) {
            addTip(tips, "fa-heartbeat", "#e3f2fd", "#1565c0", "血压偏低（" + bp + "mmHg）",
                "您的血压偏低，起床时动作宜缓，适当增加盐分摄入，多饮水，如有头晕眼花请及时就医。");
        } else {
            addTip(tips, "fa-heartbeat", "#e8f5e9", "#2e7d32", "血压正常（" + bp + "mmHg）",
                "您的血压在健康范围内，继续保持低盐饮食和规律运动的好习惯。");
        }

        if (hr > 100) {
            addTip(tips, "fa-tachometer", "#ffebee", "#c62828", "心率偏快（" + hr + "次/分）",
                "静息心率偏快，建议减少咖啡因摄入，练习腹式呼吸和冥想放松，避免过度疲劳。");
        } else if (hr < 60) {
            addTip(tips, "fa-tachometer", "#e3f2fd", "#1565c0", "心率偏慢（" + hr + "次/分）",
                "静息心率偏慢，如无不适可能为生理性心动过缓；如有乏力头晕请到医院检查。");
        } else {
            addTip(tips, "fa-tachometer", "#e8f5e9", "#2e7d32", "心率正常（" + hr + "次/分）",
                "心率在正常范围内，保持良好的作息和适度运动有助于维持健康心率。");
        }

        if (ox < 95) {
            addTip(tips, "fa-lungs", "#ffebee", "#c62828", "血氧偏低（" + ox + "%）",
                "血氧饱和度低于正常值，建议增加深呼吸练习，保持室内通风。如持续低于95%请及时就医检查肺功能。");
        } else {
            addTip(tips, "fa-lungs", "#e8f5e9", "#2e7d32", "血氧良好（" + ox + "%）",
                "血氧饱和度正常，说明心肺供氧功能良好。");
        }

        if (bp >= 130 || hr > 100) {
            addTip(tips, "fa-walking", "#e0f7fa", "#00838f", "有氧运动建议",
                "每周进行150分钟中等强度有氧运动（快走、游泳、骑行），有助于调节血压和心率。");
        }

        if (bp >= 140) {
            addTip(tips, "fa-medkit", "#fce4ec", "#c62828", "就医提醒",
                "高血压需长期规范管理，遵医嘱服药不可自行停药，建议每月测量1-2次血压。");
        }

        return tips;
    }

    private void addTip(List<Map<String, Object>> tips, String icon, String bg, String color, String title, String content) {
        Map<String, Object> tip = new HashMap<>();
        tip.put("icon", icon);
        tip.put("bg", bg);
        tip.put("color", color);
        tip.put("title", title);
        tip.put("content", content);
        tips.add(tip);
    }

    /**
     * 获取我的预约列表（自动标记逾期）
     */
    @RequestMapping(value = "/patient_portal/my_appointments", method = RequestMethod.POST)
    @ResponseBody
    public Object myAppointments() {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        wrapper.orderBy("point_date", false);
        List<DoctorPoint> list = doctorPointService.selectList(wrapper);

        // 自动标记逾期：status=0（待参与）且预约时间已过 → status=2（逾期）
        Date now = new Date();
        for (DoctorPoint point : list) {
            if ((point.getStatus() == null || point.getStatus() == 0) && point.getPointDate() != null && point.getPointDate().before(now)) {
                point.setStatus(2);
                doctorPointService.updateById(point);
            }
        }
        return list;
    }

    /**
     * 获取我的就诊历史
     */
    @RequestMapping(value = "/patient_portal/my_histories", method = RequestMethod.POST)
    @ResponseBody
    public Object myHistories() {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<PatientHistory> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        wrapper.orderBy("patient_history_date", false);
        return patientHistoryService.selectList(wrapper);
    }

    /**
     * 获取药品列表
     */
    @RequestMapping(value = "/patient_portal/medicines", method = RequestMethod.POST)
    @ResponseBody
    public Object medicines() {
        EntityWrapper<MedicineInfo> wrapper = new EntityWrapper<>();
        return medicineInfoService.selectList(wrapper);
    }

    /**
     * 获取医生列表（可按科室筛选）
     */
    @RequestMapping(value = "/patient_portal/doctors", method = RequestMethod.POST)
    @ResponseBody
    public Object doctors(String department) {
        EntityWrapper<DoctorInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("status", 1);
        if (department != null && !department.isEmpty()) {
            wrapper.eq("department", department);
        }
        wrapper.orderBy("department", true);
        return doctorInfoService.selectList(wrapper);
    }

    /**
     * 获取所有科室列表
     */
    @RequestMapping(value = "/patient_portal/departments", method = RequestMethod.POST)
    @ResponseBody
    public Object departments() {
        EntityWrapper<DoctorInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("status", 1);
        wrapper.groupBy("department");
        wrapper.setSqlSelect("DISTINCT department");
        return doctorInfoService.selectList(wrapper);
    }

    /**
     * 获取某医生在某日期已被预约的时段
     */
    @RequestMapping(value = "/patient_portal/booked_slots", method = RequestMethod.POST)
    @ResponseBody
    public Object bookedSlots(String doctorName, String date) {
        List<String> bookedSlots = new ArrayList<>();
        if (doctorName == null || doctorName.isEmpty() || date == null || date.isEmpty()) {
            return bookedSlots;
        }
        try {
            // 解析日期
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            Date dayStart = sdf.parse(date);
            Date dayEnd = new Date(dayStart.getTime() + 24 * 60 * 60 * 1000);

            // 查询该医生当天状态为待参与(0)或就诊中(3)的预约
            EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
            wrapper.eq("doctor_name", doctorName);
            wrapper.in("status", java.util.Arrays.asList(0, 3));
            wrapper.ge("point_date", dayStart);
            wrapper.lt("point_date", dayEnd);
            List<DoctorPoint> list = doctorPointService.selectList(wrapper);

            // 提取已预约的时间点（HH:mm格式）
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
            for (DoctorPoint point : list) {
                if (point.getPointDate() != null) {
                    bookedSlots.add(timeFormat.format(point.getPointDate()));
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return bookedSlots;
    }

    /**
     * 获取仪表盘统计数据
     */
    @RequestMapping(value = "/patient_portal/dashboard_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object dashboardStats() {
        ShiroUser shiroUser = ShiroKit.getUser();
        Map<String, Object> stats = new HashMap<>();

        EntityWrapper<PatientHealth> healthWrapper = new EntityWrapper<>();
        healthWrapper.eq("patient_name", shiroUser.getName());
        stats.put("myHealthCount", patientHealthService.selectCount(healthWrapper));

        EntityWrapper<DoctorPoint> appointmentWrapper = new EntityWrapper<>();
        appointmentWrapper.eq("patient_name", shiroUser.getName());
        stats.put("myAppointmentCount", doctorPointService.selectCount(appointmentWrapper));

        EntityWrapper<PatientHistory> historyWrapper = new EntityWrapper<>();
        historyWrapper.eq("patient_name", shiroUser.getName());
        stats.put("myHistoryCount", patientHistoryService.selectCount(historyWrapper));

        EntityWrapper<MedicineInfo> medicineWrapper = new EntityWrapper<>();
        stats.put("medicineCount", medicineInfoService.selectCount(medicineWrapper));

        return stats;
    }

    /**
     * 新增预约（居民端）
     */
    @RequestMapping(value = "/patient_portal/add_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object addAppointment(DoctorPoint doctorPoint) {
        try {
            ShiroUser shiroUser = ShiroKit.getUser();
            // 自动填充当前居民姓名和身份证号
            doctorPoint.setPatientName(shiroUser.getName());
            doctorPoint.setPatientIdcard(getMyPatientIdcard());
            // 默认状态：0=待参与
            if (doctorPoint.getStatus() == null) {
                doctorPoint.setStatus(0);
            }

            // 时间冲突校验：同一医生在相同时段不可重复预约
            if (doctorPoint.getDoctorName() != null && doctorPoint.getPointDate() != null) {
                EntityWrapper<DoctorPoint> conflictWrapper = new EntityWrapper<>();
                conflictWrapper.eq("doctor_name", doctorPoint.getDoctorName());
                conflictWrapper.in("status", java.util.Arrays.asList(0, 3)); // 待参与或就诊中
                // 查询该医生当天所有有效预约，再在内存中做30分钟窗口冲突判断
                List<DoctorPoint> existingList = doctorPointService.selectList(conflictWrapper);
                long requestTime = doctorPoint.getPointDate().getTime();
                for (DoctorPoint existing : existingList) {
                    if (existing.getPointDate() != null) {
                        long existingTime = existing.getPointDate().getTime();
                        // 两个预约时间差在30分钟以内则冲突
                        if (Math.abs(requestTime - existingTime) < 30 * 60 * 1000) {
                            return ResponseData.error("该医生在所选时段已有预约，请选择其他时间");
                        }
                    }
                }
            }

            doctorPointService.insert(doctorPoint);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("预约失败：" + e.getMessage());
        }
    }

    /**
     * 编辑预约（居民端）
     */
    @RequestMapping(value = "/patient_portal/update_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object updateAppointment(DoctorPoint doctorPoint) {
        try {
            ShiroUser shiroUser = ShiroKit.getUser();
            if (doctorPoint.getId() == null) {
                return ResponseData.error("缺少预约ID");
            }
            DoctorPoint existing = doctorPointService.selectById(doctorPoint.getId());
            if (existing == null) {
                return ResponseData.error("预约记录不存在");
            }
            if (!getMyPatientIdcard().equals(existing.getPatientIdcard())) {
                return ResponseData.error("无权编辑该预约");
            }
            // 保留原始患者姓名，防止篡改
            doctorPoint.setPatientName(shiroUser.getName());
            doctorPointService.updateById(doctorPoint);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("编辑失败：" + e.getMessage());
        }
    }

    /**
     * 删除预约（居民端）
     */
    @RequestMapping(value = "/patient_portal/delete_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteAppointment(Integer id) {
        ShiroUser shiroUser = ShiroKit.getUser();
        DoctorPoint point = doctorPointService.selectById(id);
        if (point != null && getMyPatientIdcard().equals(point.getPatientIdcard())) {
            point.setIsDeleted(1);
            doctorPointService.updateById(point);
            return SUCCESS_TIP;
        }
        return ResponseData.error("无权删除该预约");
    }

    /**
     * 完成预约（标记为已完成）
     */
    @RequestMapping(value = "/patient_portal/complete_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object completeAppointment(Integer id) {
        ShiroUser shiroUser = ShiroKit.getUser();
        DoctorPoint point = doctorPointService.selectById(id);
        if (point == null) {
            return ResponseData.error("预约记录不存在");
        }
        if (!getMyPatientIdcard().equals(point.getPatientIdcard())) {
            return ResponseData.error("无权操作该预约");
        }
        point.setStatus(1);
        doctorPointService.updateById(point);
        return SUCCESS_TIP;
    }

    /**
     * 新增药品（居民端）
     */
    @RequestMapping(value = "/patient_portal/add_medicine", method = RequestMethod.POST)
    @ResponseBody
    public Object addMedicine(MedicineInfo medicineInfo) {
        try {
            medicineInfoService.insert(medicineInfo);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 编辑药品（居民端）
     */
    @RequestMapping(value = "/patient_portal/update_medicine", method = RequestMethod.POST)
    @ResponseBody
    public Object updateMedicine(MedicineInfo medicineInfo) {
        try {
            if (medicineInfo.getId() == null) {
                return ResponseData.error("缺少药品ID");
            }
            MedicineInfo existing = medicineInfoService.selectById(medicineInfo.getId());
            if (existing == null) {
                return ResponseData.error("药品不存在");
            }
            medicineInfoService.updateById(medicineInfo);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("编辑失败：" + e.getMessage());
        }
    }

    /**
     * 删除药品（居民端）
     */
    @RequestMapping(value = "/patient_portal/delete_medicine", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteMedicine(Integer id) {
        try {
            MedicineInfo entity = medicineInfoService.selectById(id);
            entity.setIsDeleted(1);
            medicineInfoService.updateById(entity);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("删除失败：" + e.getMessage());
        }
    }

    // ==================== 就诊台 ====================

    /**
     * 获取我的就诊状态（候诊中/就诊中的预约）
     */
    @RequestMapping(value = "/patient_portal/my_consultation_status", method = RequestMethod.POST)
    @ResponseBody
    public Object myConsultationStatus() {
        Map<String, Object> result = new HashMap<>();

        String myIdcard = getMyPatientIdcard();

        // 候诊中（status=0 的未来预约）
        EntityWrapper<DoctorPoint> waitingWrapper = new EntityWrapper<>();
        waitingWrapper.eq("patient_idcard", myIdcard);
        waitingWrapper.eq("status", 0);
        waitingWrapper.orderBy("point_date", true);
        List<DoctorPoint> waitingList = doctorPointService.selectList(waitingWrapper);
        result.put("waitingList", waitingList);

        // 就诊中（status=3）
        EntityWrapper<DoctorPoint> inProgressWrapper = new EntityWrapper<>();
        inProgressWrapper.eq("patient_idcard", myIdcard);
        inProgressWrapper.eq("status", 3);
        List<DoctorPoint> inProgressList = doctorPointService.selectList(inProgressWrapper);
        result.put("inProgressList", inProgressList);

        // 最近完成的就诊记录（最近5条）
        EntityWrapper<PatientHistory> historyWrapper = new EntityWrapper<>();
        historyWrapper.eq("patient_idcard", myIdcard);
        historyWrapper.orderBy("patient_history_date", false);
        List<PatientHistory> recentHistory = patientHistoryService.selectList(historyWrapper);
        if (recentHistory.size() > 5) {
            recentHistory = recentHistory.subList(0, 5);
        }
        result.put("recentHistory", recentHistory);

        return result;
    }

    // ==================== 慢病管理 ====================

    /**
     * 获取我的慢病档案列表
     */
    @RequestMapping(value = "/patient_portal/my_chronic_list", method = RequestMethod.POST)
    @ResponseBody
    public Object myChronicList() {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        wrapper.eq("status", 1);
        wrapper.orderBy("update_time", false);
        return chronicDiseaseService.selectList(wrapper);
    }

    /**
     * 获取我的随访记录
     */
    @RequestMapping(value = "/patient_portal/my_chronic_followups", method = RequestMethod.POST)
    @ResponseBody
    public Object myChronicFollowups(Integer chronicId) {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<ChronicFollowup> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        if (chronicId != null) {
            wrapper.eq("chronic_id", chronicId);
        }
        wrapper.orderBy("followup_date", false);
        return chronicFollowupService.selectList(wrapper);
    }

    /**
     * 获取我的随访计划
     */
    @RequestMapping(value = "/patient_portal/my_chronic_plans", method = RequestMethod.POST)
    @ResponseBody
    public Object myChronicPlans() {
        ShiroUser shiroUser = ShiroKit.getUser();
        // 自动标记过期
        EntityWrapper<ChronicFollowupPlan> expiredWrapper = new EntityWrapper<>();
        expiredWrapper.eq("patient_name", shiroUser.getName());
        expiredWrapper.eq("status", 0);
        expiredWrapper.lt("plan_date", new Date());
        List<ChronicFollowupPlan> expiredPlans = chronicFollowupPlanService.selectList(expiredWrapper);
        for (ChronicFollowupPlan plan : expiredPlans) {
            plan.setStatus(2);
            chronicFollowupPlanService.updateById(plan);
        }

        EntityWrapper<ChronicFollowupPlan> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_idcard", getMyPatientIdcard());
        wrapper.orderBy("plan_date", true);
        return chronicFollowupPlanService.selectList(wrapper);
    }

    // ==================== 公共卫生（居民个人） ====================

    @RequestMapping(value = "/patient_portal/my_vaccinations", method = RequestMethod.POST)
    @ResponseBody
    public Object myVaccinations() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.VaccinationRecord> w = new EntityWrapper<>();
        w.eq("patient_idcard", getMyPatientIdcard());
        w.eq("is_deleted", 0);
        w.orderBy("vacc_date", false);
        return vaccinationRecordMapper.selectList(w);
    }

    @RequestMapping(value = "/patient_portal/my_checkups", method = RequestMethod.POST)
    @ResponseBody
    public Object myElderlyCheckups() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.ElderlyCheckup> w = new EntityWrapper<>();
        w.eq("patient_idcard", getMyPatientIdcard());
        w.eq("is_deleted", 0);
        w.orderBy("checkup_date", false);
        return elderlyCheckupMapper.selectList(w);
    }

    @RequestMapping(value = "/patient_portal/my_vaccinations_by_name", method = RequestMethod.POST)
    @ResponseBody
    public Object myVaccinationsByName() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.VaccinationRecord> w = new EntityWrapper<>();
        w.eq("patient_name", ShiroKit.getUser().getName());
        w.eq("is_deleted", 0);
        w.orderBy("vacc_date", false);
        return vaccinationRecordMapper.selectList(w);
    }

    @RequestMapping(value = "/patient_portal/my_checkups_by_name", method = RequestMethod.POST)
    @ResponseBody
    public Object myElderlyCheckupsByName() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.ElderlyCheckup> w = new EntityWrapper<>();
        w.eq("patient_name", ShiroKit.getUser().getName());
        w.eq("is_deleted", 0);
        w.orderBy("checkup_date", false);
        return elderlyCheckupMapper.selectList(w);
    }
}
