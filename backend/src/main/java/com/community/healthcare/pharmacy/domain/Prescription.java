package com.community.healthcare.pharmacy.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 处方聚合，封装线下接诊后开方、药师审方、配药核对与发药状态机。
 *
 * <p>签署前必须关联接诊、诊断和至少一条明细；已发药或已作废处方不可再次作废。</p>
 */
public final class Prescription {
    private final UUID id;
    private final UUID encounterId;
    private final String diagnosis;
    private final List<PrescriptionLine> lines;
    private final Instant createdAt;
    private PrescriptionStatus status;
    private Instant updatedAt;
    private String reviewNote;
    private String voidReason;

    private Prescription(UUID id,
                         UUID encounterId,
                         String diagnosis,
                         List<PrescriptionLine> lines,
                         Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "处方号不能为空");
        this.encounterId = encounterId;
        this.diagnosis = diagnosis == null ? null : diagnosis.trim();
        this.lines = List.copyOf(Objects.requireNonNull(lines, "处方明细不能为空"));
        this.status = PrescriptionStatus.DRAFT;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    /** 创建尚可修改的处方草稿。 */
    public static Prescription draft(UUID id,
                                     UUID encounterId,
                                     String diagnosis,
                                     List<PrescriptionLine> lines,
                                     Clock clock) {
        return new Prescription(id, encounterId, diagnosis, lines, now(clock));
    }

    /** 校验处方完整性并由医生签署。 */
    public void sign(Clock clock) {
        requireStatus(PrescriptionStatus.DRAFT, "签署");
        if (encounterId == null) {
            throw new IllegalStateException("处方签署前必须关联接诊记录");
        }
        if (diagnosis == null || diagnosis.isBlank()) {
            throw new IllegalStateException("处方签署前必须填写诊断");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("处方签署前必须包含至少一条明细");
        }
        transition(PrescriptionStatus.SIGNED, clock);
    }

    /** 将已签署处方提交给药师审方。 */
    public void submitForReview(Clock clock) {
        requireStatus(PrescriptionStatus.SIGNED, "提交审方");
        transition(PrescriptionStatus.PENDING_REVIEW, clock);
    }

    public void approve(Clock clock) {
        requireStatus(PrescriptionStatus.PENDING_REVIEW, "审方通过");
        transition(PrescriptionStatus.APPROVED, clock);
    }

    public void reject(String note, Clock clock) {
        requireStatus(PrescriptionStatus.PENDING_REVIEW, "审方拒绝");
        reviewNote = requireText(note, "审方拒绝原因不能为空");
        transition(PrescriptionStatus.REJECTED, clock);
    }

    public void startPicking(Clock clock) {
        requireStatus(PrescriptionStatus.APPROVED, "配药");
        transition(PrescriptionStatus.PICKING, clock);
    }

    public void check(Clock clock) {
        requireStatus(PrescriptionStatus.PICKING, "核对");
        transition(PrescriptionStatus.CHECKED, clock);
    }

    public void dispense(Clock clock) {
        requireStatus(PrescriptionStatus.CHECKED, "发药");
        transition(PrescriptionStatus.DISPENSED, clock);
    }

    /** 在发药完成前按原因作废处方。 */
    public void voidPrescription(String reason, Clock clock) {
        if (status == PrescriptionStatus.DISPENSED || status == PrescriptionStatus.VOID) {
            throw new IllegalStateException("处方状态 " + status + " 不能作废");
        }
        voidReason = requireText(reason, "处方作废原因不能为空");
        transition(PrescriptionStatus.VOID, clock);
    }

    private void requireStatus(PrescriptionStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("处方状态 " + status + " 不能执行" + action);
        }
    }

    private void transition(PrescriptionStatus target, Clock clock) {
        status = target;
        updatedAt = now(clock);
    }

    private static String requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() { return id; }
    public Optional<UUID> encounterId() { return Optional.ofNullable(encounterId); }
    public Optional<String> diagnosis() { return Optional.ofNullable(diagnosis); }
    public List<PrescriptionLine> lines() { return lines; }
    public PrescriptionStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Optional<String> reviewNote() { return Optional.ofNullable(reviewNote); }
    public Optional<String> voidReason() { return Optional.ofNullable(voidReason); }
}
