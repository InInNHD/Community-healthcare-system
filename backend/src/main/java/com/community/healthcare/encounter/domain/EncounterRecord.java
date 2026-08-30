package com.community.healthcare.encounter.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 可签署、可追溯更正的线下接诊记录聚合。
 *
 * <p>草稿允许原位编辑；签署后正文不可修改，只能创建指向上一版本的新更正记录，
 * 原版本转为 {@link EncounterStatus#AMENDED}。作废同样保留原记录和原因，满足医疗文书追溯要求。</p>
 */
public final class EncounterRecord {
    private final UUID id;
    private final UUID appointmentId;
    private final int version;
    private final UUID previousVersionId;
    private final String amendmentReason;
    private String body;
    private EncounterStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant signedAt;
    private String voidReason;

    private EncounterRecord(UUID id,
                            UUID appointmentId,
                            int version,
                            UUID previousVersionId,
                            String amendmentReason,
                            String body,
                            Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "接诊号不能为空");
        this.appointmentId = Objects.requireNonNull(appointmentId, "预约号不能为空");
        this.version = version;
        this.previousVersionId = previousVersionId;
        this.amendmentReason = amendmentReason;
        this.body = requireText(body, "接诊正文不能为空");
        this.status = EncounterStatus.DRAFT;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    /** 创建版本号为 1 的接诊草稿。 */
    public static EncounterRecord draft(UUID id, UUID appointmentId, String body, Clock clock) {
        return new EncounterRecord(id, appointmentId, 1, null, null, body, now(clock));
    }

    public void editDraftBody(String changedBody, Clock clock) {
        requireStatus(EncounterStatus.DRAFT, "编辑");
        body = requireText(changedBody, "接诊正文不能为空");
        updatedAt = now(clock);
    }

    /**
     * 签署草稿并产生预约完成流程可消费的领域事件。
     *
     * @throws IllegalStateException 当前记录不是草稿
     */
    public EncounterSignedEvent sign(Clock clock) {
        requireStatus(EncounterStatus.DRAFT, "签署");
        Instant occurredAt = now(clock);
        status = EncounterStatus.SIGNED;
        signedAt = occurredAt;
        updatedAt = occurredAt;
        return new EncounterSignedEvent(id, appointmentId, occurredAt);
    }

    /**
     * 为已签署记录创建下一版本，并把当前版本标记为已更正。
     *
     * @return 独立标识、版本号递增且指向当前记录的新草稿
     */
    public EncounterRecord amend(UUID correctionId,
                                 String correctedBody,
                                 String reason,
                                 Clock clock) {
        requireStatus(EncounterStatus.SIGNED, "更正");
        Objects.requireNonNull(correctionId, "更正版本接诊号不能为空");
        if (id.equals(correctionId)) {
            throw new IllegalArgumentException("更正版本必须使用新的接诊号");
        }
        String checkedReason = requireText(reason, "更正原因不能为空");
        Instant occurredAt = now(clock);
        EncounterRecord correction = new EncounterRecord(
                correctionId,
                appointmentId,
                version + 1,
                id,
                checkedReason,
                correctedBody,
                occurredAt);
        status = EncounterStatus.AMENDED;
        updatedAt = occurredAt;
        return correction;
    }

    /** 作废已签署记录并保留原因；作废不会物理删除医疗文书。 */
    public void voidRecord(String reason, Clock clock) {
        requireStatus(EncounterStatus.SIGNED, "作废");
        voidReason = requireText(reason, "作废原因不能为空");
        status = EncounterStatus.VOID;
        updatedAt = now(clock);
    }

    private void requireStatus(EncounterStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("接诊记录状态 " + status + " 不能执行" + action);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() {
        return id;
    }

    public UUID appointmentId() {
        return appointmentId;
    }

    public int version() {
        return version;
    }

    public Optional<UUID> previousVersionId() {
        return Optional.ofNullable(previousVersionId);
    }

    public Optional<String> amendmentReason() {
        return Optional.ofNullable(amendmentReason);
    }

    public String body() {
        return body;
    }

    public EncounterStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> signedAt() {
        return Optional.ofNullable(signedAt);
    }

    public Optional<String> voidReason() {
        return Optional.ofNullable(voidReason);
    }
}
