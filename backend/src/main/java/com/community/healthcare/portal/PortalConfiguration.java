package com.community.healthcare.portal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 居民门户公开展示所需的机构服务信息。
 *
 * <p>所有字段在启动期校验，避免门户加载后才暴露缺失配置。</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.portal")
record PortalProperties(
        @NotBlank String organizationName,
        @NotBlank String servicePhone,
        @NotBlank String serviceHours,
        @NotBlank String emergencyPhone) {
}

/** 注册并启用门户配置属性绑定。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PortalProperties.class)
class PortalPropertiesConfiguration {
}

/**
 * 无需认证即可读取的门户基础配置接口。
 *
 * <p>响应只包含可公开的机构名称和服务电话，不返回内部网络或安全配置。</p>
 */
@RestController
@RequestMapping("/api/public")
class PortalConfigurationController {
    private final PortalProperties properties;

    PortalConfigurationController(PortalProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/portal-config")
    PortalConfigurationResponse portalConfig() {
        return new PortalConfigurationResponse(properties.organizationName(), properties.servicePhone(),
                properties.serviceHours(), properties.emergencyPhone());
    }
}

/** 居民门户展示使用的公开配置响应。 */
record PortalConfigurationResponse(
        String organizationName,
        String servicePhone,
        String serviceHours,
        String emergencyPhone) {
}
