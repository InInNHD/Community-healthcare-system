package com.community.healthcare.integration;

/** 区域卫生平台出站适配端口；真实接口可在不影响业务层的情况下替换当前模拟实现。 */
public interface RegionalHealthPlatformPort {
    /** 以外部可识别的幂等键提交标准化 JSON 载荷。 */
    ExchangeReceipt submit(String idempotencyKey, String payloadJson);
}
