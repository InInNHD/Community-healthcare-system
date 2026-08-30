package com.community.healthcare.portal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PortalConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PortalPropertiesConfiguration.class);

    @Test
    void refusesToStartWithoutProductionPortalPresentationValues() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void bindsAllRequiredPortalPresentationValues() {
        contextRunner.withPropertyValues(
                        "app.portal.organization-name=东城社区卫生服务中心",
                        "app.portal.service-phone=010-87654321",
                        "app.portal.service-hours=工作日 08:00-17:00",
                        "app.portal.emergency-phone=120")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    PortalProperties properties = context.getBean(PortalProperties.class);
                    assertThat(properties.organizationName()).isEqualTo("东城社区卫生服务中心");
                    assertThat(properties.servicePhone()).isEqualTo("010-87654321");
                });
    }
}
