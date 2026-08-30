package cn.stylefeng.guns.modular.doctor_portal.controller;

import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.core.shiro.ShiroUser;
import cn.stylefeng.guns.modular.doctor_info.service.IDoctorInfoService;
import cn.stylefeng.guns.modular.doctor_point.service.IDoctorPointService;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医护端门户控制器
 *
 * @author guns
 */
@Controller
public class DoctorPortalController extends BaseController {

    @Autowired
    private IDoctorPointService doctorPointService;

    @Autowired
    private IDoctorInfoService doctorInfoService;

    @Autowired
    private IPatientHealthService patientHealthService;

    @Autowired
    private IPatientHistoryService patientHistoryService;

    @Autowired
    private IMedicineInfoService medicineInfoService;

    @Autowired
    private IPatientInfoService patientInfoService;

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

/**
     * 获取当前医生所属科室的所有医生姓名列表
     */
    private List<String> getDepartmentDoctorNames() {
        return getDepartmentDoctorNamesInternal(false);
    }

    /**
     * 获取当前医生所属科室的所有医生ID列表
     */
    private List<Integer> getDepartmentDoctorIds() {
        return getDepartmentDoctorIdsInternal();
    }

    /**
     * 获取当前登录医生的 doctor_info 记录
     */
    private DoctorInfo getCurrentDoctor() {
        Integer userId = ShiroKit.getUser().getId();
        EntityWrapper<DoctorInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("status", 1);
        List<DoctorInfo> list = doctorInfoService.selectList(wrapper);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        // 降级：按名字查找
        EntityWrapper<DoctorInfo> nameWrapper = new EntityWrapper<>();
        nameWrapper.eq("doctor_name", ShiroKit.getUser().getName());
        nameWrapper.eq("status", 1);
        List<DoctorInfo> nameList = doctorInfoService.selectList(nameWrapper);
        return (nameList != null && !nameList.isEmpty()) ? nameList.get(0) : null;
    }

    private List<String> getDepartmentDoctorNamesInternal(boolean dummy) {
        DoctorInfo self = getCurrentDoctor();
        if (self == null) {
            List<String> names = new ArrayList<>();
            names.add(ShiroKit.getUser().getName());
            return names;
        }
        EntityWrapper<DoctorInfo> deptWrapper = new EntityWrapper<>();
        deptWrapper.eq("department", self.getDepartment());
        deptWrapper.eq("status", 1);
        List<DoctorInfo> deptDoctors = doctorInfoService.selectList(deptWrapper);
        List<String> names = new ArrayList<>();
        for (DoctorInfo doc : deptDoctors) {
            names.add(doc.getDoctorName());
        }
        if (names.isEmpty()) {
            names.add(ShiroKit.getUser().getName());
        }
        return names;
    }

    private List<Integer> getDepartmentDoctorIdsInternal() {
        DoctorInfo self = getCurrentDoctor();
        if (self == null) {
            List<Integer> ids = new ArrayList<>();
            ids.add(0);
            return ids;
        }
        EntityWrapper<DoctorInfo> deptWrapper = new EntityWrapper<>();
        deptWrapper.eq("department", self.getDepartment());
        deptWrapper.eq("status", 1);
        List<DoctorInfo> deptDoctors = doctorInfoService.selectList(deptWrapper);
        List<Integer> ids = new ArrayList<>();
        for (DoctorInfo doc : deptDoctors) {
            ids.add(doc.getId());
        }
        if (ids.isEmpty()) {
            ids.add(self.getId());
        }
        return ids;
    }

    /**
     * 跳转到医护端门户首页
     */
    @RequestMapping(value = "/doctor_portal", method = RequestMethod.GET)
    public String index(Model model) {
        ShiroUser shiroUser = ShiroKit.getUser();
        model.addAttribute("userName", shiroUser.getName());
        model.addAttribute("userRole", "医护人员");

        //统计概览数据
        EntityWrapper<DoctorPoint> appointmentWrapper = new EntityWrapper<>();
        appointmentWrapper.eq("doctor_name", shiroUser.getName());
        int myAppointmentCount = doctorPointService.selectCount(appointmentWrapper);

        EntityWrapper<PatientHealth> healthWrapper = new EntityWrapper<>();
        int healthRecordCount = patientHealthService.selectCount(healthWrapper);

        EntityWrapper<PatientHistory> historyWrapper = new EntityWrapper<>();
        historyWrapper.eq("patient_doctor", shiroUser.getName());
        int myPatientCount = patientHistoryService.selectCount(historyWrapper);

        EntityWrapper<MedicineInfo> medicineWrapper = new EntityWrapper<>();
        int medicineCount = medicineInfoService.selectCount(medicineWrapper);

        model.addAttribute("myAppointmentCount", myAppointmentCount);
        model.addAttribute("healthRecordCount", healthRecordCount);
        model.addAttribute("myPatientCount", myPatientCount);
        model.addAttribute("medicineCount", medicineCount);

        return "/doctor_portal.html";
    }

    /**
     * 获取我的预约列表（科室范围内）
     */
    @RequestMapping(value = "/doctor_portal/appointments", method = RequestMethod.POST)
    @ResponseBody
    public Object appointments() {
        List<String> doctorNames = getDepartmentDoctorNames();
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.in("doctor_name", doctorNames);
        wrapper.orderBy("point_date", false);
        return doctorPointService.selectList(wrapper);
    }

    /**
     * 获取所有居民健康记录
     */
    @RequestMapping(value = "/doctor_portal/health_records", method = RequestMethod.POST)
    @ResponseBody
    public Object healthRecords() {
        EntityWrapper<PatientHealth> wrapper = new EntityWrapper<>();
        wrapper.orderBy("date", false);
        return patientHealthService.selectList(wrapper);
    }

    /**
     * 获取我的就诊历史
     */
    @RequestMapping(value = "/doctor_portal/patient_histories", method = RequestMethod.POST)
    @ResponseBody
    public Object patientHistories() {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<PatientHistory> wrapper = new EntityWrapper<>();
        wrapper.eq("patient_doctor", shiroUser.getName());
        wrapper.orderBy("patient_history_date", false);
        return patientHistoryService.selectList(wrapper);
    }

    /**
     * 获取药品列表
     */
    @RequestMapping(value = "/doctor_portal/medicines", method = RequestMethod.POST)
    @ResponseBody
    public Object medicines() {
        EntityWrapper<MedicineInfo> wrapper = new EntityWrapper<>();
        return medicineInfoService.selectList(wrapper);
    }

    /**
     * 获取预约列表（科室范围内，用于管理）
     */
    @RequestMapping(value = "/doctor_portal/all_appointments", method = RequestMethod.POST)
    @ResponseBody
    public Object allAppointments() {
        List<String> doctorNames = getDepartmentDoctorNames();
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.in("doctor_name", doctorNames);
        wrapper.orderBy("point_date", false);
        return doctorPointService.selectList(wrapper);
    }

    /**
     * 获取就诊记录（科室范围内，用于管理）
     */
    @RequestMapping(value = "/doctor_portal/all_histories", method = RequestMethod.POST)
    @ResponseBody
    public Object allHistories() {
        List<String> doctorNames = getDepartmentDoctorNames();
        EntityWrapper<PatientHistory> wrapper = new EntityWrapper<>();
        wrapper.in("patient_doctor", doctorNames);
        wrapper.orderBy("patient_history_date", false);
        return patientHistoryService.selectList(wrapper);
    }

    /**
     * 获取仪表盘统计数据（科室范围内）
     */
    @RequestMapping(value = "/doctor_portal/dashboard_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object dashboardStats() {
        List<String> doctorNames = getDepartmentDoctorNames();
        Map<String, Object> stats = new HashMap<>();

        EntityWrapper<DoctorPoint> appointmentWrapper = new EntityWrapper<>();
        appointmentWrapper.in("doctor_name", doctorNames);
        stats.put("myAppointmentCount", doctorPointService.selectCount(appointmentWrapper));

        EntityWrapper<PatientHealth> healthWrapper = new EntityWrapper<>();
        stats.put("healthRecordCount", patientHealthService.selectCount(healthWrapper));

        EntityWrapper<PatientHistory> historyWrapper = new EntityWrapper<>();
        historyWrapper.in("patient_doctor", doctorNames);
        stats.put("myPatientCount", patientHistoryService.selectCount(historyWrapper));

        EntityWrapper<MedicineInfo> medicineWrapper = new EntityWrapper<>();
        stats.put("medicineCount", medicineInfoService.selectCount(medicineWrapper));

        return stats;
    }

    /**
     * 新增预约（医护端）
     */
    @RequestMapping(value = "/doctor_portal/add_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object addAppointment(DoctorPoint doctorPoint) {
        try {
            ShiroUser shiroUser = ShiroKit.getUser();
            doctorPoint.setDoctorName(shiroUser.getName());
            // 时间冲突校验：同一医生在相同时段不可重复预约
            if (doctorPoint.getPointDate() != null) {
                EntityWrapper<DoctorPoint> conflictWrapper = new EntityWrapper<>();
                conflictWrapper.eq("doctor_name", shiroUser.getName());
                conflictWrapper.in("status", java.util.Arrays.asList(0, 3));
                List<DoctorPoint> existingList = doctorPointService.selectList(conflictWrapper);
                long requestTime = doctorPoint.getPointDate().getTime();
                for (DoctorPoint existing : existingList) {
                    if (existing.getPointDate() != null) {
                        long existingTime = existing.getPointDate().getTime();
                        if (Math.abs(requestTime - existingTime) < 30 * 60 * 1000) {
                            return ResponseData.error("您在所选时段已有预约，请选择其他时间");
                        }
                    }
                }
            }

            doctorPointService.insert(doctorPoint);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 新增健康记录（医护端）
     */
    @RequestMapping(value = "/doctor_portal/add_health", method = RequestMethod.POST)
    @ResponseBody
    public Object addHealth(PatientHealth patientHealth) {
        try {
            patientHealthService.insert(patientHealth);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 新增就诊记录（医护端）
     */
    @RequestMapping(value = "/doctor_portal/add_history", method = RequestMethod.POST)
    @ResponseBody
    public Object addHistory(PatientHistory patientHistory) {
        try {
            ShiroUser shiroUser = ShiroKit.getUser();
            patientHistory.setPatientDoctor(shiroUser.getName());
            patientHistoryService.insert(patientHistory);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 新增药品（医护端）
     */
    @RequestMapping(value = "/doctor_portal/add_medicine", method = RequestMethod.POST)
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
     * 编辑预约（医护端）
     */
    @RequestMapping(value = "/doctor_portal/update_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object updateAppointment(DoctorPoint doctorPoint) {
        try {
            if (doctorPoint.getId() == null) {
                return ResponseData.error("缺少预约ID");
            }
            DoctorPoint existing = doctorPointService.selectById(doctorPoint.getId());
            if (existing == null) {
                return ResponseData.error("预约记录不存在");
            }
            doctorPointService.updateById(doctorPoint);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("编辑失败：" + e.getMessage());
        }
    }

    /**
     * 删除预约（医护端）
     */
    @RequestMapping(value = "/doctor_portal/delete_appointment", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteAppointment(Integer id) {
        DoctorPoint entity = doctorPointService.selectById(id);
        entity.setIsDeleted(1);
        doctorPointService.updateById(entity);
        return SUCCESS_TIP;
    }

    /**
     * 删除健康记录（医护端）
     */
    @RequestMapping(value = "/doctor_portal/delete_health", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteHealth(Integer id) {
        PatientHealth entity = patientHealthService.selectById(id);
        entity.setIsDeleted(1);
        patientHealthService.updateById(entity);
        return SUCCESS_TIP;
    }

    /**
     * 删除就诊记录（医护端）
     */
    @RequestMapping(value = "/doctor_portal/delete_history", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteHistory(Integer id) {
        PatientHistory entity = patientHistoryService.selectById(id);
        entity.setIsDeleted(1);
        patientHistoryService.updateById(entity);
        return SUCCESS_TIP;
    }

    /**
     * 编辑药品（医护端）
     */
    @RequestMapping(value = "/doctor_portal/update_medicine", method = RequestMethod.POST)
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
     * 删除药品（医护端）
     */
    @RequestMapping(value = "/doctor_portal/delete_medicine", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteMedicine(Integer id) {
        MedicineInfo entity = medicineInfoService.selectById(id);
        entity.setIsDeleted(1);
        medicineInfoService.updateById(entity);
        return SUCCESS_TIP;
    }

    // ==================== 就诊台 ====================

    /**
     * 获取候诊队列（科室范围内所有医生的待参与预约，按时间升序）
     */
    @RequestMapping(value = "/doctor_portal/consultation_queue", method = RequestMethod.POST)
    @ResponseBody
    public Object consultationQueue() {
        List<String> doctorNames = getDepartmentDoctorNames();
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.in("doctor_name", doctorNames);
        wrapper.eq("status", 0);
        wrapper.orderBy("point_date", true);
        return doctorPointService.selectList(wrapper);
    }

    /**
     * 获取当前就诊中的患者（仅当前医生自己的）
     */
    @RequestMapping(value = "/doctor_portal/current_patient", method = RequestMethod.POST)
    @ResponseBody
    public Object currentPatient() {
        ShiroUser shiroUser = ShiroKit.getUser();
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.eq("doctor_name", shiroUser.getName());
        wrapper.eq("status", 3);
        wrapper.orderBy("point_date", true);
        List<DoctorPoint> list = doctorPointService.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取当前登录医生姓名（前端用于判断是否可接诊）
     */
    @RequestMapping(value = "/doctor_portal/my_name", method = RequestMethod.POST)
    @ResponseBody
    public Object myName() {
        ShiroUser shiroUser = ShiroKit.getUser();
        Map<String, Object> result = new HashMap<>();
        result.put("doctorName", shiroUser.getName());
        return result;
    }

    /**
     * 接诊（将预约状态从待参与改为就诊中）
     */
    @RequestMapping(value = "/doctor_portal/start_consultation", method = RequestMethod.POST)
    @ResponseBody
    public Object startConsultation(Integer appointmentId) {
        DoctorPoint point = doctorPointService.selectById(appointmentId);
        if (point == null) {
            return ResponseData.error("预约记录不存在");
        }
        ShiroUser shiroUser = ShiroKit.getUser();
        if (!shiroUser.getName().equals(point.getDoctorName())) {
            return ResponseData.error("无权操作该预约");
        }
        if (point.getStatus() != null && point.getStatus() != 0) {
            return ResponseData.error("该预约不在待参与状态");
        }

        // 时间冲突校验：同一医生同时只能接诊一名患者
        EntityWrapper<DoctorPoint> inProgressWrapper = new EntityWrapper<>();
        inProgressWrapper.eq("doctor_name", shiroUser.getName());
        inProgressWrapper.eq("status", 3);
        int inProgressCount = doctorPointService.selectCount(inProgressWrapper);
        if (inProgressCount > 0) {
            return ResponseData.error("您当前已有就诊中的患者，请先完成当前就诊再接诊下一位");
        }

        point.setStatus(3);
        doctorPointService.updateById(point);
        return SUCCESS_TIP;
    }

    /**
     * 完成就诊（填写诊断信息，生成就诊记录，将预约标记为已完成）
     */
    @RequestMapping(value = "/doctor_portal/finish_consultation", method = RequestMethod.POST)
    @ResponseBody
    public Object finishConsultation(Integer appointmentId, String patientSym, String patientMedicine, Integer takeprice) {
        DoctorPoint point = doctorPointService.selectById(appointmentId);
        if (point == null) {
            return ResponseData.error("预约记录不存在");
        }
        ShiroUser shiroUser = ShiroKit.getUser();
        if (!shiroUser.getName().equals(point.getDoctorName())) {
            return ResponseData.error("无权操作该预约");
        }
        if (point.getStatus() == null || point.getStatus() != 3) {
            return ResponseData.error("该预约不在就诊中状态");
        }

        // 生成就诊记录
        PatientHistory history = new PatientHistory();
        history.setPatientName(point.getPatientName());
        history.setPatientIdcard(point.getPatientIdcard());
        history.setPatientDoctor(shiroUser.getName());
        history.setPatientSym(patientSym);
        history.setPatientMedicine(patientMedicine);
        history.setTakeprice(takeprice != null ? takeprice : 0);
        history.setPatientHistoryDate(new Date());
        patientHistoryService.insert(history);

        // 更新预约状态为已完成
        point.setStatus(1);
        doctorPointService.updateById(point);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "就诊完成，已生成就诊记录");
        result.put("historyId", history.getId());
        return result;
    }

    /**
     * 获取今日就诊统计（科室范围内）
     */
    @RequestMapping(value = "/doctor_portal/consultation_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object consultationStats() {
        List<String> doctorNames = getDepartmentDoctorNames();
        Map<String, Object> stats = new HashMap<>();

        EntityWrapper<DoctorPoint> waitingWrapper = new EntityWrapper<>();
        waitingWrapper.in("doctor_name", doctorNames);
        waitingWrapper.eq("status", 0);
        stats.put("waitingCount", doctorPointService.selectCount(waitingWrapper));

        EntityWrapper<DoctorPoint> inProgressWrapper = new EntityWrapper<>();
        inProgressWrapper.in("doctor_name", doctorNames);
        inProgressWrapper.eq("status", 3);
        stats.put("inProgressCount", doctorPointService.selectCount(inProgressWrapper));

        EntityWrapper<DoctorPoint> completedWrapper = new EntityWrapper<>();
        completedWrapper.in("doctor_name", doctorNames);
        completedWrapper.eq("status", 1);
        stats.put("completedCount", doctorPointService.selectCount(completedWrapper));

        return stats;
    }

    // ==================== 慢病管理 ====================

    /**
     * 获取慢病档案列表（科室范围内）
     */
    @RequestMapping(value = "/doctor_portal/chronic_list", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicList(String diseaseType, String riskLevel) {
        List<Integer> doctorIds = getDepartmentDoctorIds();
        EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
        wrapper.in("doctor_id", doctorIds);
        if (diseaseType != null && !diseaseType.isEmpty()) {
            wrapper.eq("disease_type", diseaseType);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq("risk_level", riskLevel);
        }
        wrapper.orderBy("risk_level", false);
        wrapper.orderBy("update_time", false);
        return chronicDiseaseService.selectList(wrapper);
    }

    /**
     * 新增慢病档案（医护端）
     */
    @RequestMapping(value = "/doctor_portal/add_chronic", method = RequestMethod.POST)
    @ResponseBody
    public Object addChronic(ChronicDisease chronicDisease) {
        try {
            DoctorInfo currentDoc = getCurrentDoctor();
            chronicDisease.setDoctorName(currentDoc != null ? currentDoc.getDoctorName() : ShiroKit.getUser().getName());
            chronicDisease.setDoctorId(currentDoc != null ? currentDoc.getId() : null);
            chronicDisease.setCreateTime(new Date());
            chronicDisease.setUpdateTime(new Date());
            if (chronicDisease.getStatus() == null) {
                chronicDisease.setStatus(1);
            }
            chronicDiseaseService.insert(chronicDisease);

            // 自动生成随访计划（使用服务层方法）
            chronicFollowupService.generateFollowupPlan(chronicDisease);

            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 编辑慢病档案（医护端）
     */
    @RequestMapping(value = "/doctor_portal/update_chronic", method = RequestMethod.POST)
    @ResponseBody
    public Object updateChronic(ChronicDisease chronicDisease) {
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

            // 风险等级变更时重新生成随访计划
            if (chronicDisease.getRiskLevel() != null && !chronicDisease.getRiskLevel().equals(existing.getRiskLevel())) {
                EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
                planWrapper.eq("chronic_id", chronicDisease.getId());
                planWrapper.eq("status", 0);
                chronicFollowupPlanService.delete(planWrapper);
                ChronicDisease updated = chronicDiseaseService.selectById(chronicDisease.getId());
                chronicFollowupService.generateFollowupPlan(updated);
            }

            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("编辑失败：" + e.getMessage());
        }
    }

    /**
     * 删除慢病档案（医护端）
     */
    @RequestMapping(value = "/doctor_portal/delete_chronic", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteChronic(Integer id) {
        try {
            // 级联软删除随访计划
            EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
            planWrapper.eq("chronic_id", id);
            List<ChronicFollowupPlan> plans = chronicFollowupPlanService.selectList(planWrapper);
            for (ChronicFollowupPlan plan : plans) {
                plan.setIsDeleted(1);
                chronicFollowupPlanService.updateById(plan);
            }

            // 级联软删除随访记录
            EntityWrapper<ChronicFollowup> followupWrapper = new EntityWrapper<>();
            followupWrapper.eq("chronic_id", id);
            List<ChronicFollowup> followups = chronicFollowupService.selectList(followupWrapper);
            for (ChronicFollowup fw : followups) {
                fw.setIsDeleted(1);
                chronicFollowupService.updateById(fw);
            }

            // 软删除档案
            ChronicDisease entity = chronicDiseaseService.selectById(id);
            entity.setIsDeleted(1);
            chronicDiseaseService.updateById(entity);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取随访记录列表
     */
    @RequestMapping(value = "/doctor_portal/chronic_followup_list", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicFollowupList(Integer chronicId) {
        EntityWrapper<ChronicFollowup> wrapper = new EntityWrapper<>();
        if (chronicId != null) {
            wrapper.eq("chronic_id", chronicId);
        }
        wrapper.orderBy("followup_date", false);
        return chronicFollowupService.selectList(wrapper);
    }

    /**
     * 新增随访记录（医护端）- 同时自动生成下次随访计划
     */
    @RequestMapping(value = "/doctor_portal/add_chronic_followup", method = RequestMethod.POST)
    @ResponseBody
    public Object addChronicFollowup(ChronicFollowup followup) {
        try {
            DoctorInfo currentDoc = getCurrentDoctor();
            followup.setFollowupDoctor(currentDoc != null ? currentDoc.getDoctorName() : ShiroKit.getUser().getName());
            followup.setFollowupDoctorId(currentDoc != null ? currentDoc.getId() : null);
            return chronicFollowupService.executeFollowup(followup, null);
        } catch (Exception e) {
            return ResponseData.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 获取随访计划列表
     */
    @RequestMapping(value = "/doctor_portal/chronic_followup_plans", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicFollowupPlans(Integer chronicId) {
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
     * 获取慢病统计数据
     */
    @RequestMapping(value = "/doctor_portal/chronic_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicStats() {
        return chronicDiseaseService.getStatsByIds(getDepartmentDoctorIds());
    }

    // ==================== 公共卫生 ====================

    @RequestMapping(value = "/doctor_portal/vaccination_list", method = RequestMethod.POST)
    @ResponseBody
    public Object vaccinationList(String patientName) {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.VaccinationRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (patientName != null && !patientName.isEmpty()) w.like("patient_name", patientName);
        w.orderBy("vacc_date", false);
        return vaccinationRecordMapper.selectList(w);
    }

    @RequestMapping(value = "/doctor_portal/maternal_list", method = RequestMethod.POST)
    @ResponseBody
    public Object maternalList() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.MaternalRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        w.orderBy("create_time", false);
        return maternalRecordMapper.selectList(w);
    }

    @RequestMapping(value = "/doctor_portal/elderly_checkups", method = RequestMethod.POST)
    @ResponseBody
    public Object elderlyCheckups() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.ElderlyCheckup> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        w.orderBy("checkup_date", false);
        return elderlyCheckupMapper.selectList(w);
    }

    @RequestMapping(value = "/doctor_portal/infectious_reports", method = RequestMethod.POST)
    @ResponseBody
    public Object infectiousReports() {
        EntityWrapper<cn.stylefeng.guns.modular.system.model.InfectiousDiseaseReport> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        w.orderBy("report_date", false);
        return infectiousDiseaseReportMapper.selectList(w);
    }

    @RequestMapping(value = "/doctor_portal/public_health_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object publicHealthStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("vaccinationCount", vaccinationRecordMapper.selectCount(null));
        s.put("maternalCount", maternalRecordMapper.selectCount(null));
        s.put("elderlyCount", elderlyCheckupMapper.selectCount(null));
        s.put("infectiousCount", infectiousDiseaseReportMapper.selectCount(null));
        return s;
    }

}
