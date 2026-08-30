package com.community.healthcare.insurance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 医保申报聚合。
 *
 * <p>当前实现明确标记为模拟申报；将来接入真实医保适配器时仍复用相同的状态约束和外部受理号。</p>
 */
public final class InsuranceClaim {
    private final UUID id;
    private final UUID invoiceId;
    private final BigDecimal claimedAmount;
    private final boolean simulation;
    private final Instant createdAt;
    private InsuranceClaimStatus status;
    private Instant updatedAt;
    private String externalReference;
    private String rejectionReason;

    private InsuranceClaim(UUID id, UUID invoiceId, BigDecimal claimedAmount, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "医保理赔号不能为空");
        this.invoiceId = Objects.requireNonNull(invoiceId, "账单号不能为空");
        this.claimedAmount = positiveMoney(claimedAmount);
        this.simulation = true;
        this.status = InsuranceClaimStatus.DRAFT;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    /** 创建与账单关联的模拟医保申报。 */
    public static InsuranceClaim simulated(UUID id, UUID invoiceId, BigDecimal amount, Clock clock) {
        return new InsuranceClaim(id, invoiceId, amount, now(clock));
    }

    public void submit(Clock clock) {
        transition(InsuranceClaimStatus.DRAFT, InsuranceClaimStatus.SUBMITTED, clock);
    }

    public void accept(String reference, Clock clock) {
        requireStatus(InsuranceClaimStatus.SUBMITTED, "受理");
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("模拟医保受理号不能为空");
        }
        externalReference = reference.trim();
        setStatus(InsuranceClaimStatus.ACCEPTED, clock);
    }

    public void reject(String reason, Clock clock) {
        requireStatus(InsuranceClaimStatus.SUBMITTED, "拒付");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("拒付原因不能为空");
        }
        rejectionReason = reason.trim();
        setStatus(InsuranceClaimStatus.REJECTED, clock);
    }

    public void settle(Clock clock) {
        transition(InsuranceClaimStatus.ACCEPTED, InsuranceClaimStatus.SETTLED, clock);
    }

    private void transition(InsuranceClaimStatus expected, InsuranceClaimStatus target, Clock clock) {
        requireStatus(expected, "转换为 " + target);
        setStatus(target, clock);
    }

    private void requireStatus(InsuranceClaimStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("医保理赔状态 " + status + " 不能执行" + action);
        }
    }

    private void setStatus(InsuranceClaimStatus target, Clock clock) {
        status = target;
        updatedAt = now(clock);
    }

    private static BigDecimal positiveMoney(BigDecimal amount) {
        Objects.requireNonNull(amount, "申报金额不能为空");
        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("申报金额最多保留两位小数", exception);
        }
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("申报金额必须大于零");
        }
        return normalized;
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() { return id; }
    public UUID invoiceId() { return invoiceId; }
    public BigDecimal claimedAmount() { return claimedAmount; }
    public boolean simulation() { return simulation; }
    public InsuranceClaimStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Optional<String> externalReference() { return Optional.ofNullable(externalReference); }
    public Optional<String> rejectionReason() { return Optional.ofNullable(rejectionReason); }
}
