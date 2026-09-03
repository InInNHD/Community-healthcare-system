package com.community.healthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区医疗后端的 Spring Boot 启动入口。
 *
 * <p>组件扫描以 {@code com.community.healthcare} 为根，所有业务模块在同一进程中运行，
 * 但通过包边界和应用服务维持模块化单体结构。</p>
 */
@SpringBootApplication
@EnableScheduling
public class HealthcareApplication {
    /**
     * 启动 Web 应用并加载当前激活 Profile 的配置。
     *
     * @param args 传递给 Spring Boot 的命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HealthcareApplication.class, args);
    }
}
