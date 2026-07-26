package cn.stylefeng.guns.modular.admin_portal.controller;

import cn.stylefeng.guns.core.common.constant.state.ManagerStatus;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.core.shiro.ShiroUser;
import cn.stylefeng.guns.modular.doctor_info.service.IDoctorInfoService;
import cn.stylefeng.guns.modular.doctor_point.service.IDoctorPointService;
import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineInfoService;
import cn.stylefeng.guns.modular.pateint_health_manager.service.IPatientHealthService;
import cn.stylefeng.guns.modular.patient.service.IPatientInfoService;
import cn.stylefeng.guns.modular.patient_history_manager.service.IPatientHistoryService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicDiseaseService;
import cn.stylefeng.guns.modular.system.dao.VaccinationRecordMapper;
import cn.stylefeng.guns.modular.system.dao.MaternalRecordMapper;
import cn.stylefeng.guns.modular.system.dao.ElderlyCheckupMapper;
import cn.stylefeng.guns.modular.system.dao.InfectiousDiseaseReportMapper;
import cn.stylefeng.guns.modular.system.model.DoctorInfo;
import cn.stylefeng.guns.modular.system.model.DoctorPoint;
import cn.stylefeng.guns.modular.system.model.MedicineInfo;
import cn.stylefeng.guns.modular.system.model.PatientHealth;
import cn.stylefeng.guns.modular.system.model.PatientHistory;
import cn.stylefeng.guns.modular.system.model.PatientInfo;
import cn.stylefeng.guns.modular.system.model.User;
import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.service.IUserService;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 管理员端门户控制器
 */
@Controller
public class AdminPortalController extends BaseController {

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
    private IUserService userService;

    @Autowired
    private IChronicDiseaseService chronicDiseaseService;

    @Autowired private VaccinationRecordMapper vaccinationRecordMapper;
    @Autowired private MaternalRecordMapper maternalRecordMapper;
    @Autowired private ElderlyCheckupMapper elderlyCheckupMapper;
    @Autowired private InfectiousDiseaseReportMapper infectiousDiseaseReportMapper;

/**
     * 跳转到管理员端门户首页
     */
    @RequestMapping(value = "/admin_portal", method = RequestMethod.GET)
    public String index(Model model) {
        ShiroUser shiroUser = ShiroKit.getUser();
        model.addAttribute("userName", shiroUser.getName());
        model.addAttribute("userRole", "管理员");

        // 统计概览数据
        int patientCount = patientInfoService.selectCount(new EntityWrapper<>());
        int doctorCount = doctorInfoService.selectCount(new EntityWrapper<DoctorInfo>().eq("status", 1));
        int appointmentCount = doctorPointService.selectCount(new EntityWrapper<>());
        int medicineCount = medicineInfoService.selectCount(new EntityWrapper<>());

        model.addAttribute("patientCount", patientCount);
        model.addAttribute("doctorCount", doctorCount);
        model.addAttribute("appointmentCount", appointmentCount);
        model.addAttribute("medicineCount", medicineCount);

        return "/admin_portal.html";
    }

    /**
     * 获取仪表盘统计数据
     */
    @RequestMapping(value = "/admin_portal/dashboard_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("patientCount", patientInfoService.selectCount(new EntityWrapper<>()));
        stats.put("doctorCount", doctorInfoService.selectCount(new EntityWrapper<DoctorInfo>().eq("status", 1)));
        stats.put("appointmentCount", doctorPointService.selectCount(new EntityWrapper<>()));
        stats.put("medicineCount", medicineInfoService.selectCount(new EntityWrapper<>()));
        stats.put("healthRecordCount", patientHealthService.selectCount(new EntityWrapper<>()));
        stats.put("historyCount", patientHistoryService.selectCount(new EntityWrapper<>()));
        return stats;
    }

    /**
     * 获取最近预约列表
     */
    @RequestMapping(value = "/admin_portal/recent_appointments", method = RequestMethod.POST)
    @ResponseBody
    public Object recentAppointments() {
        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        wrapper.orderBy("point_date", false);
        wrapper.last("LIMIT 10");
        return doctorPointService.selectList(wrapper);
    }

    /**
     * 获取最近就诊记录
     */
    @RequestMapping(value = "/admin_portal/recent_histories", method = RequestMethod.POST)
    @ResponseBody
    public Object recentHistories() {
        EntityWrapper<PatientHistory> wrapper = new EntityWrapper<>();
        wrapper.orderBy("patient_history_date", false);
        wrapper.last("LIMIT 10");
        return patientHistoryService.selectList(wrapper);
    }

    // ==================== 公共卫生统计 ====================
    @RequestMapping(value = "/admin_portal/public_health_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object publicHealthStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("vaccinationCount", vaccinationRecordMapper.selectCount(null));
        s.put("maternalCount", maternalRecordMapper.selectCount(null));
        s.put("highRiskMaternal", 0);
        s.put("elderlyCount", elderlyCheckupMapper.selectCount(null));
        s.put("infectiousCount", infectiousDiseaseReportMapper.selectCount(null));
        s.put("pendingInfectious", 0);
        return s;
    }

    /**
     * 获取所有医生信息（含关联账号信息）
     */
    @RequestMapping(value = "/admin_portal/doctor_list", method = RequestMethod.POST)
    @ResponseBody
    public Object doctorList() {
        List<DoctorInfo> doctors = doctorInfoService.selectList(new EntityWrapper<DoctorInfo>().orderBy("id", true));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DoctorInfo doc : doctors) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", doc.getId());
            item.put("doctorName", doc.getDoctorName());
            item.put("department", doc.getDepartment());
            item.put("title", doc.getTitle());
            item.put("specialty", doc.getSpecialty());
            item.put("status", doc.getStatus());
            item.put("userId", doc.getUserId());
            item.put("hasAccount", doc.getUserId() != null);
            if (doc.getUserId() != null) {
                User user = userService.selectById(doc.getUserId());
                if (user != null) {
                    item.put("account", user.getAccount());
                    item.put("phone", user.getPhone());
                    item.put("userStatus", user.getStatus());
                }
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 获取所有居民信息（含关联账号信息）
     */
    @RequestMapping(value = "/admin_portal/patient_list", method = RequestMethod.POST)
    @ResponseBody
    public Object patientList() {
        List<PatientInfo> patients = patientInfoService.selectList(new EntityWrapper<PatientInfo>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (PatientInfo pat : patients) {
            Map<String, Object> item = new HashMap<>();
            item.put("paientIdcard", pat.getPaientIdcard());
            item.put("paientName", pat.getPaientName());
            item.put("paientMoney", pat.getPaientMoney());
            item.put("userId", pat.getUserId());
            item.put("hasAccount", pat.getUserId() != null);
            if (pat.getUserId() != null) {
                User user = userService.selectById(pat.getUserId());
                if (user != null) {
                    item.put("account", user.getAccount());
                    item.put("phone", user.getPhone());
                    item.put("userStatus", user.getStatus());
                }
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 同步医生账号：为所有未关联账号的医生生成登录账号
     */
    @RequestMapping(value = "/admin_portal/sync_doctors", method = RequestMethod.POST)
    @ResponseBody
    public ResponseData syncDoctors() {
        List<DoctorInfo> doctors = doctorInfoService.selectList(
                new EntityWrapper<DoctorInfo>().isNull("user_id"));
        int created = 0;
        for (DoctorInfo doc : doctors) {
            // 生成账号：doctor_ + id
            String account = "doctor_" + doc.getId();
            // 检查账号是否已存在
            User existUser = userService.getByAccount(account);
            if (existUser != null) {
                // 账号已存在，关联到该用户
                doc.setUserId(existUser.getId());
                doctorInfoService.updateById(doc);
                continue;
            }
            // 创建sys_user记录
            User user = new User();
            user.setAccount(account);
            String salt = ShiroKit.getRandomSalt(5);
            user.setSalt(salt);
            user.setPassword(ShiroKit.md5("111111", salt));
            user.setName(doc.getDoctorName());
            user.setRoleid("5"); // 医生角色
            user.setDeptid(25); // 医生部门
            user.setStatus(ManagerStatus.OK.getCode());
            user.setCreatetime(new Date());
            userService.insert(user);

            // 关联user_id
            doc.setUserId(user.getId());
            doctorInfoService.updateById(doc);
            created++;
        }
        return ResponseData.success(created);
    }

    /**
     * 同步居民账号：为所有未关联账号的居民生成登录账号
     */
    @RequestMapping(value = "/admin_portal/sync_patients", method = RequestMethod.POST)
    @ResponseBody
    public ResponseData syncPatients() {
        List<PatientInfo> patients = patientInfoService.selectList(
                new EntityWrapper<PatientInfo>().isNull("user_id"));
        int created = 0;
        for (PatientInfo pat : patients) {
            // 生成账号：patient_ + 身份证后6位
            String idSuffix = pat.getPaientIdcard();
            if (idSuffix != null && idSuffix.length() > 6) {
                idSuffix = idSuffix.substring(idSuffix.length() - 6);
            }
            String account = "patient_" + idSuffix;
            // 检查账号是否已存在
            User existUser = userService.getByAccount(account);
            if (existUser != null) {
                pat.setUserId(existUser.getId());
                patientInfoService.updateById(pat);
                continue;
            }
            // 创建sys_user记录
            User user = new User();
            user.setAccount(account);
            String salt = ShiroKit.getRandomSalt(5);
            user.setSalt(salt);
            user.setPassword(ShiroKit.md5("111111", salt));
            user.setName(pat.getPaientName());
            user.setRoleid("6"); // 病人角色
            user.setDeptid(26); // 病人部门
            user.setStatus(ManagerStatus.OK.getCode());
            user.setCreatetime(new Date());
            userService.insert(user);

            // 关联user_id
            pat.setUserId(user.getId());
            patientInfoService.updateById(pat);
            created++;
        }
        return ResponseData.success(created);
    }

    /**
     * 获取所有预约列表（含筛选，管理员可查看全部预约）
     */
    @RequestMapping(value = "/admin_portal/appointment_list", method = RequestMethod.POST)
    @ResponseBody
    public Object appointmentList() {
        String statusFilter = super.getPara("status");
        String doctorFilter = super.getPara("doctorName");
        String patientFilter = super.getPara("patientName");

        EntityWrapper<DoctorPoint> wrapper = new EntityWrapper<>();
        if (statusFilter != null && !statusFilter.isEmpty()) {
            wrapper.eq("status", Integer.parseInt(statusFilter));
        }
        if (doctorFilter != null && !doctorFilter.isEmpty()) {
            wrapper.like("doctor_name", doctorFilter);
        }
        if (patientFilter != null && !patientFilter.isEmpty()) {
            wrapper.like("patient_name", patientFilter);
        }
        wrapper.orderBy("point_date", false);

        List<DoctorPoint> list = doctorPointService.selectList(wrapper);

        // 自动标记逾期：status=0且预约时间已过 → status=2
        Date now = new Date();
        for (DoctorPoint point : list) {
            if ((point.getStatus() == null || point.getStatus() == 0) && point.getPointDate() != null && point.getPointDate().before(now)) {
                point.setStatus(2);
                doctorPointService.updateById(point);
            }
        }

        // 构建返回结果，附加来源标识
        List<Map<String, Object>> result = new ArrayList<>();
        for (DoctorPoint point : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", point.getId());
            item.put("patientName", point.getPatientName());
            item.put("patientIdcard", point.getPatientIdcard());
            item.put("doctorName", point.getDoctorName());
            item.put("doctorId", point.getDoctorId());
            item.put("pointDate", point.getPointDate());
            item.put("pointPlace", point.getPointPlace());
            item.put("status", point.getStatus());

            // 来源判断：有doctor_id且doctor_name与某位医生的user关联，则来源为医护端
            // 否则来源为居民端
            String source = "居民端";
            if (point.getDoctorId() != null) {
                DoctorInfo doc = doctorInfoService.selectById(point.getDoctorId());
                if (doc != null && doc.getUserId() != null) {
                    // 如果预约的医生有关联账号且doctor_id有值，可能是医护端创建的
                    User doctorUser = userService.selectById(doc.getUserId());
                    if (doctorUser != null && doctorUser.getAccount() != null
                            && doctorUser.getAccount().startsWith("doctor_")) {
                        // 如果预约是通过医护端创建的，doctorName就是创建者，且patientIdcard可能是"0"
                        // 但实际上两个端都可以创建预约，我们通过patientIdcard判断
                    }
                }
                source = "医护端";
            }
            // 更精确的来源判断：如果patientIdcard为"0"或空，说明是医护端代创建
            if (point.getPatientIdcard() != null && "0".equals(point.getPatientIdcard())) {
                source = "医护端";
            }
            item.put("source", source);

            result.add(item);
        }
        return result;
    }

    /**
     * 获取预约统计数据
     */
    @RequestMapping(value = "/admin_portal/appointment_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object appointmentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", doctorPointService.selectCount(new EntityWrapper<>()));

        EntityWrapper<DoctorPoint> waitingWrapper = new EntityWrapper<>();
        waitingWrapper.eq("status", 0);
        // 待参与：status=0且预约时间未过
        waitingWrapper.gt("point_date", new Date());
        stats.put("waiting", doctorPointService.selectCount(waitingWrapper));

        EntityWrapper<DoctorPoint> completedWrapper = new EntityWrapper<>();
        completedWrapper.eq("status", 1);
        stats.put("completed", doctorPointService.selectCount(completedWrapper));

        EntityWrapper<DoctorPoint> overdueWrapper = new EntityWrapper<>();
        overdueWrapper.eq("status", 2);
        stats.put("overdue", doctorPointService.selectCount(overdueWrapper));

        EntityWrapper<DoctorPoint> inProgressWrapper = new EntityWrapper<>();
        inProgressWrapper.eq("status", 3);
        stats.put("inProgress", doctorPointService.selectCount(inProgressWrapper));

        return stats;
    }

    // ==================== 慢病管理 ====================

    /**
     * 获取所有慢病档案（管理员端）
     */
    @RequestMapping(value = "/admin_portal/chronic_list", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicList(String diseaseType, String riskLevel, String patientName) {
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
        wrapper.orderBy("risk_level", false);
        wrapper.orderBy("update_time", false);
        return chronicDiseaseService.selectList(wrapper);
    }

    /**
     * 获取慢病统计数据（管理员端）
     */
    @RequestMapping(value = "/admin_portal/chronic_stats", method = RequestMethod.POST)
    @ResponseBody
    public Object chronicStats() {
        return chronicDiseaseService.getStats(null);
    }
}
