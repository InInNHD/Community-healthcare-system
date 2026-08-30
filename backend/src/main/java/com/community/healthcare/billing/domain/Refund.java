package com.community.healthcare.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 一笔不可变的账单退款流水。 */
public record Refund(UUID id, UUID invoiceId, BigDecimal amount, Instant occurredAt) {
    public Refund {
        Objects.requireNonNull(id, "退款流水号不能为空");
        Objects.requireNonNull(invoiceId, "账单号不能为空");
        Objects.requireNonNull(amount, "退款金额不能为空");
        Objects.requireNonNull(occurredAt, "退款时间不能为空");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("退款金额必须大于零");
        }
    }
}
