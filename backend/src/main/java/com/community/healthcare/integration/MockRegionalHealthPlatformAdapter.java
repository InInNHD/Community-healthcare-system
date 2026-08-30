package com.community.healthcare.integration;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域卫生平台的内存模拟适配器。
 *
 * <p>同一幂等键始终返回同一模拟受理号，可用于验证重试与对账流程。</p>
 */
@Component
public class MockRegionalHealthPlatformAdapter implements RegionalHealthPlatformPort {
    private final Map<String, ExchangeReceipt> receipts = new ConcurrentHashMap<>();

    /** 模拟提交；载荷含测试失败标记时抛出暂时不可用异常。 */
    @Override
    public ExchangeReceipt submit(String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("幂等键不能为空");
        if (payloadJson != null && payloadJson.contains("simulateFailure")) {
            throw new RegionalPlatformUnavailableException("模拟区域平台暂不可用");
        }
        return receipts.computeIfAbsent(idempotencyKey, key -> new ExchangeReceipt(
                "SIM-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)), true, true));
    }
}
