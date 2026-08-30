package com.community.healthcare.encounter.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 接诊记录完成签署的领域事件。
 *
 * <p>预约聚合只接受属于自身的事件，以确保“完成接诊”必须由真实签署动作驱动。</p>
 */
public record EncounterSignedEvent(UUID encounterId, UUID appointmentId, Instant signedAt) {
    public EncounterSignedEvent {
        Objects.requireNonNull(encounterId, "接诊号不能为空");
        Objects.requireNonNull(appointmentId, "预约号不能为空");
        Objects.requireNonNull(signedAt, "签署时间不能为空");
    }
}
