package cn.stylefeng.guns.config;

import cn.stylefeng.guns.core.common.constant.Const;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.modular.system.dao.UserMapper;
import cn.stylefeng.guns.modular.system.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 数据初始化配置
 * 启动时重置指定账号的密码为默认密码 111111
 */
@Configuration
public class DataInitConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitConfig.class);

    @Autowired
    private UserMapper userMapper;

    private static final List<String> RESET_ACCOUNTS = Arrays.asList("admin", "doctor", "patient");

    @Override
    public void run(String... args) {
        for (String account : RESET_ACCOUNTS) {
            User user = userMapper.getByAccount(account);
            if (user != null) {
                String correctPassword = ShiroKit.md5(Const.DEFAULT_PWD, user.getSalt());
                if (!correctPassword.equals(user.getPassword())) {
                    user.setPassword(correctPassword);
                    userMapper.updateById(user);
                    logger.info("已重置账号 [{}] 的密码为默认密码", account);
                }
            }
        }
    }
}
