package com.community.healthcare.billing.domain;

/** 账单从草稿、出账、支付到退款的生命周期状态。 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    REFUNDED
}
