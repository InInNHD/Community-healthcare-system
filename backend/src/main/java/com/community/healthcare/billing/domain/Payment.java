package com.community.healthcare.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 一笔不可变的账单支付流水。 */
public record Payment(UUID id, UUID invoiceId, BigDecimal amount, Instant occurredAt) {
    public Payment {
        Objects.requireNonNull(id, "支付流水号不能为空");
        Objects.requireNonNull(invoiceId, "账单号不能为空");
        Objects.requireNonNull(amount, "支付金额不能为空");
        Objects.requireNonNull(occurredAt, "支付时间不能为空");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("支付金额必须大于零");
        }
    }
}
