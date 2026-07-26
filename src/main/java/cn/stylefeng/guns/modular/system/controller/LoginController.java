
package cn.stylefeng.guns.modular.system.controller;

import cn.stylefeng.guns.core.common.exception.InvalidKaptchaException;
import cn.stylefeng.guns.core.log.LogManager;
import cn.stylefeng.guns.core.log.factory.LogTaskFactory;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.core.shiro.ShiroUser;
import cn.stylefeng.guns.core.util.KaptchaUtil;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.util.ToolUtil;
import com.google.code.kaptcha.Constants;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static cn.stylefeng.roses.core.util.HttpContext.getIp;

/**
 * 登录控制器
 *
 * @author 
 * @Date 
 */
@Controller
public class LoginController extends BaseController {

    /**
     * 跳转到主页（根据角色重定向到不同门户）
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {
        List<Integer> roleList = ShiroKit.getUser().getRoleList();
        if (roleList == null || roleList.size() == 0) {
            ShiroKit.getSubject().logout();
            return REDIRECT + "/login";
        }

        //根据角色判断跳转到不同门户
        ShiroUser shiroUser = ShiroKit.getUser();
        List<String> roleNames = shiroUser.getRoleNames();
        if (roleNames != null && roleNames.size() > 0) {
            String primaryRole = roleNames.get(0);
            //医生角色 -> 医护端
            if ("医生".equals(primaryRole)) {
                return REDIRECT + "/doctor_portal";
            }
            //病人角色 -> 居民端
            if ("病人".equals(primaryRole)) {
                return REDIRECT + "/patient_portal";
            }
            //管理员角色 -> 管理员端门户
            if ("超级管理员".equals(primaryRole)) {
                return REDIRECT + "/admin_portal";
            }
        }

        //其他角色 -> 管理员端门户
        return REDIRECT + "/admin_portal";
    }

    /**
     * 跳转到登录页面
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        if (ShiroKit.isAuthenticated() || ShiroKit.getUser() != null) {
            return REDIRECT + "/";
        } else {
            return "/login.html";
        }
    }

    /**
     * 点击登录执行的动作
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginVali() {

        String username = super.getPara("username").trim();
        String password = super.getPara("password").trim();
        String remember = super.getPara("remember");

        //验证验证码是否正确
        if (KaptchaUtil.getKaptchaOnOff()) {
            String kaptcha = super.getPara("kaptcha").trim();
            String code = (String) super.getSession().getAttribute(Constants.KAPTCHA_SESSION_KEY);
            if (ToolUtil.isEmpty(kaptcha) || !kaptcha.equalsIgnoreCase(code)) {
                throw new InvalidKaptchaException();
            }
        }

        Subject currentUser = ShiroKit.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password.toCharArray());

        if ("on".equals(remember)) {
            token.setRememberMe(true);
        } else {
            token.setRememberMe(false);
        }

        currentUser.login(token);

        ShiroUser shiroUser = ShiroKit.getUser();
        super.getSession().setAttribute("shiroUser", shiroUser);
        super.getSession().setAttribute("username", shiroUser.getAccount());

        LogManager.me().executeLog(LogTaskFactory.loginLog(shiroUser.getId(), getIp()));

        ShiroKit.getSession().setAttribute("sessionFlag", true);

        //根据角色判断重定向到不同门户
        List<String> roleNames = shiroUser.getRoleNames();
        if (roleNames != null && roleNames.size() > 0) {
            String primaryRole = roleNames.get(0);
            if ("医生".equals(primaryRole)) {
                return REDIRECT + "/doctor_portal";
            }
            if ("病人".equals(primaryRole)) {
                return REDIRECT + "/patient_portal";
            }
            if ("超级管理员".equals(primaryRole)) {
                return REDIRECT + "/admin_portal";
            }
        }

        return REDIRECT + "/admin_portal";
    }

    /**
     * 退出登录
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logOut() {
        LogManager.me().executeLog(LogTaskFactory.exitLog(ShiroKit.getUser().getId(), getIp()));
        ShiroKit.getSubject().logout();
        deleteAllCookie();
        return REDIRECT + "/login";
    }
}
