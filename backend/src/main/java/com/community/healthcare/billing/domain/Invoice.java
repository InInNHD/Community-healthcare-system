package com.community.healthcare.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 账单聚合，集中维护出账、分次支付和退款约束。
 *
 * <p>金额统一精确到两位小数；累计支付不得超过账单总额，累计退款不得超过实付金额。</p>
 */
public final class Invoice {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final UUID id;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private InvoiceStatus status;
    private BigDecimal paidAmount = ZERO;
    private BigDecimal refundedAmount = ZERO;
    private Instant updatedAt;

    private Invoice(UUID id, BigDecimal totalAmount, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "账单号不能为空");
        this.totalAmount = positiveMoney(totalAmount, "账单金额必须大于零");
        this.status = InvoiceStatus.DRAFT;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    /** 创建尚未对居民生效的草稿账单。 */
    public static Invoice draft(UUID id, BigDecimal totalAmount, Clock clock) {
        return new Invoice(id, totalAmount, now(clock));
    }

    /** 正式出账，使账单进入可支付状态。 */
    public void issue(Clock clock) {
        requireStatus(InvoiceStatus.DRAFT, "出账");
        status = InvoiceStatus.ISSUED;
        updatedAt = now(clock);
    }

    /** 登记一笔支付并按累计金额更新为部分支付或已支付。 */
    public Payment pay(UUID paymentId, BigDecimal amount, Clock clock) {
        if (status != InvoiceStatus.ISSUED && status != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("账单状态 " + status + " 不能支付");
        }
        BigDecimal checkedAmount = positiveMoney(amount, "支付金额必须大于零");
        if (paidAmount.add(checkedAmount).compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("支付金额超额");
        }
        Instant occurredAt = now(clock);
        paidAmount = paidAmount.add(checkedAmount);
        status = paidAmount.compareTo(totalAmount) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        updatedAt = occurredAt;
        return new Payment(paymentId, id, checkedAmount, occurredAt);
    }

    /** 对已全额支付账单退款；累计全退时将账单标记为已退款。 */
    public Refund refund(UUID refundId, BigDecimal amount, Clock clock) {
        requireStatus(InvoiceStatus.PAID, "退款");
        BigDecimal checkedAmount = positiveMoney(amount, "退款金额必须大于零");
        if (refundedAmount.add(checkedAmount).compareTo(paidAmount) > 0) {
            throw new IllegalArgumentException("退款金额超额");
        }
        Instant occurredAt = now(clock);
        refundedAmount = refundedAmount.add(checkedAmount);
        if (refundedAmount.compareTo(paidAmount) == 0) {
            status = InvoiceStatus.REFUNDED;
        }
        updatedAt = occurredAt;
        return new Refund(refundId, id, checkedAmount, occurredAt);
    }

    private void requireStatus(InvoiceStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("账单状态 " + status + " 不能执行" + action);
        }
    }

    private static BigDecimal positiveMoney(BigDecimal amount, String message) {
        Objects.requireNonNull(amount, message);
        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("金额最多保留两位小数", exception);
        }
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() { return id; }
    public BigDecimal totalAmount() { return totalAmount; }
    public InvoiceStatus status() { return status; }
    public BigDecimal paidAmount() { return paidAmount; }
    public BigDecimal refundedAmount() { return refundedAmount; }
    public BigDecimal netPaidAmount() { return paidAmount.subtract(refundedAmount); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
