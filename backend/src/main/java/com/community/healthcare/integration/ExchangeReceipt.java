package com.community.healthcare.integration;

/** 外部平台交换回执，包含外部受理号、受理结果及是否为模拟实现。 */
public record ExchangeReceipt(String externalReference, boolean accepted, boolean simulation) {
}
