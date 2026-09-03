package com.community.healthcare.integration;

import com.community.healthcare.referral.application.R5PlatformService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时认领并投递本地事务出站事件，失败退避和死信记录由应用服务统一维护。 */
@Component
@ConditionalOnProperty(name = "app.integration.outbox-dispatch.enabled", havingValue = "true")
public class OutboxDispatcher {
    private final R5PlatformService service;

    public OutboxDispatcher(R5PlatformService service) {
        this.service = service;
    }

    /** 单次最多处理 20 条，避免外部平台异常时长期占用调度线程。 */
    @Scheduled(fixedDelayString = "${app.integration.outbox-dispatch.poll-delay-ms:5000}")
    public void dispatch() {
        service.dueOutboxIds(20).forEach(service::dispatchDueOutbox);
    }
}
