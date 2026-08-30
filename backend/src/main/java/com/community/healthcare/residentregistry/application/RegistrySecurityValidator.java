package com.community.healthcare.residentregistry.application;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 在生产环境启动时验证居民标识 HMAC pepper 的强度。
 *
 * <p>pepper 与数据库分离保存；泄露数据库时，攻击者仍不能仅凭常见证件号空间批量反查哈希。</p>
 */
@Component
class RegistrySecurityValidator implements InitializingBean {
    static final String DEFAULT_PEPPER = "demo-r1-identifier-pepper-change-in-production";
    private final String pepper;
    private final Environment environment;

    RegistrySecurityValidator(@Value("${app.registry.identifier-pepper:" + DEFAULT_PEPPER + "}") String pepper,
                              Environment environment) {
        this.pepper = pepper;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && (DEFAULT_PEPPER.equals(pepper) || pepper.length() < 32)) {
            throw new IllegalStateException("生产环境必须配置至少 32 字符且非默认的 identifier pepper");
        }
    }
}
