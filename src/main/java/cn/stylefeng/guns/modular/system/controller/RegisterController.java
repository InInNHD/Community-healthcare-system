package cn.stylefeng.guns.modular.system.controller;

import cn.stylefeng.guns.core.common.constant.state.ManagerStatus;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.modular.system.dao.DoctorInfoMapper;
import cn.stylefeng.guns.modular.system.dao.PatientInfoMapper;
import cn.stylefeng.guns.modular.system.model.DoctorInfo;
import cn.stylefeng.guns.modular.system.model.PatientInfo;
import cn.stylefeng.guns.modular.system.model.User;
import cn.stylefeng.guns.modular.system.service.IUserService;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import cn.stylefeng.roses.core.util.ToolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
 * 注册控制器
 */
@Controller
public class RegisterController extends BaseController {

    @Autowired
    private IUserService userService;

    @Autowired
    private DoctorInfoMapper doctorInfoMapper;

    @Autowired
    private PatientInfoMapper patientInfoMapper;

    /**
     * 跳转到注册页面
     */
    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String register(Model model) {
        if (ShiroKit.isAuthenticated() || ShiroKit.getUser() != null) {
            return REDIRECT + "/";
        }
        return "/register.html";
    }

    /**
     * 医护端注册
     */
    @RequestMapping(value = "/register/doctor", method = RequestMethod.POST)
    @ResponseBody
    public ResponseData registerDoctor() {
        String account = super.getPara("account");
        String password = super.getPara("password");
        String confirmPassword = super.getPara("confirmPassword");
        String name = super.getPara("name");
        String department = super.getPara("department");
        String title = super.getPara("title");
        String phone = super.getPara("phone");

        // 安全trim
        account = account == null ? "" : account.trim();
        password = password == null ? "" : password.trim();
        confirmPassword = confirmPassword == null ? "" : confirmPassword.trim();
        name = name == null ? "" : name.trim();
        department = department == null ? "" : department.trim();
        title = title == null ? "" : title.trim();
        phone = phone == null ? "" : phone.trim();

        // 校验必填项
        if (ToolUtil.isEmpty(account) || ToolUtil.isEmpty(password) || ToolUtil.isEmpty(name)) {
            return ResponseData.error("账号、密码和姓名为必填项");
        }

        // 校验两次密码是否一致
        if (!password.equals(confirmPassword)) {
            return ResponseData.error("两次输入的密码不一致");
        }

        // 校验账号是否已存在
        User theUser = userService.getByAccount(account);
        if (theUser != null) {
            return ResponseData.error("该账号已存在");
        }

        // 创建sys_user记录
        User user = new User();
        user.setAccount(account);
        user.setSalt(ShiroKit.getRandomSalt(5));
        user.setPassword(ShiroKit.md5(password, user.getSalt()));
        user.setName(name);
        user.setPhone(phone);
        user.setRoleid("5"); // 医生角色ID
        user.setStatus(ManagerStatus.OK.getCode());
        user.setCreatetime(new Date());
        userService.insert(user);

        // 创建doctor_info记录，doctor_name与sys_user.name保持一致
        DoctorInfo doctorInfo = new DoctorInfo();
        doctorInfo.setDoctorName(name);
        doctorInfo.setDepartment(department);
        doctorInfo.setTitle(title);
        doctorInfo.setStatus(1); // 正常状态
        doctorInfo.setUserId(user.getId()); // 关联sys_user
        doctorInfoMapper.insert(doctorInfo);

        return SUCCESS_TIP;
    }

    /**
     * 居民端注册
     */
    @RequestMapping(value = "/register/patient", method = RequestMethod.POST)
    @ResponseBody
    public ResponseData registerPatient() {
        String account = super.getPara("account");
        String password = super.getPara("password");
        String confirmPassword = super.getPara("confirmPassword");
        String name = super.getPara("name");
        String idcard = super.getPara("idcard");
        String phone = super.getPara("phone");

        // 安全trim
        account = account == null ? "" : account.trim();
        password = password == null ? "" : password.trim();
        confirmPassword = confirmPassword == null ? "" : confirmPassword.trim();
        name = name == null ? "" : name.trim();
        idcard = idcard == null ? "" : idcard.trim();
        phone = phone == null ? "" : phone.trim();

        // 校验必填项
        if (ToolUtil.isEmpty(account) || ToolUtil.isEmpty(password) || ToolUtil.isEmpty(name)) {
            return ResponseData.error("账号、密码和姓名为必填项");
        }

        if (ToolUtil.isEmpty(idcard)) {
            return ResponseData.error("身份证号为必填项");
        }

        // 校验两次密码是否一致
        if (!password.equals(confirmPassword)) {
            return ResponseData.error("两次输入的密码不一致");
        }

        // 校验账号是否已存在
        User theUser = userService.getByAccount(account);
        if (theUser != null) {
            return ResponseData.error("该账号已存在");
        }

        // 校验身份证号是否已存在
        PatientInfo existPatient = patientInfoMapper.selectById(idcard);
        if (existPatient != null) {
            return ResponseData.error("该身份证号已注册");
        }

        // 创建sys_user记录
        User user = new User();
        user.setAccount(account);
        user.setSalt(ShiroKit.getRandomSalt(5));
        user.setPassword(ShiroKit.md5(password, user.getSalt()));
        user.setName(name);
        user.setPhone(phone);
        user.setRoleid("6"); // 病人角色ID
        user.setStatus(ManagerStatus.OK.getCode());
        user.setCreatetime(new Date());
        userService.insert(user);

        // 创建patient_info记录，paient_name与sys_user.name保持一致
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPaientIdcard(idcard);
        patientInfo.setPaientName(name);
        patientInfo.setPaientMoney("0");
        patientInfo.setUserId(user.getId()); // 关联sys_user
        patientInfoMapper.insert(patientInfo);

        return SUCCESS_TIP;
    }
}
