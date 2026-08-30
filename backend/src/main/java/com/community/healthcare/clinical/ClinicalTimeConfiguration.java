package com.community.healthcare.clinical;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 为临床规则提供可注入、可测试的业务时钟。 */
@Configuration(proxyBeanMethods = false)
class ClinicalTimeConfiguration {
    @Bean
    Clock businessClock(@Value("${app.business-zone:Asia/Shanghai}") String businessZone) {
        return Clock.system(ZoneId.of(businessZone));
    }
}

/** 使用配置的业务时区校验居民预约时间。 */
@Component
class AppointmentTimePolicy {
    private final Clock clock;

    AppointmentTimePolicy(Clock clock) {
        this.clock = clock;
    }

    /** 要求预约时间严格晚于业务时钟当前时间。 */
    void requireFuture(LocalDateTime scheduledAt) {
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("预约时间必须是业务时区内的未来时间");
        }
    }
}
