package com.community.healthcare.scheduling.domain;

import com.community.healthcare.encounter.domain.EncounterSignedEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 预约生命周期聚合。
 *
 * <p>状态必须按“待确认→已确认→已签到→接诊中→已完成”推进；取消和爽约只允许从明确的前置状态发生。
 * 完成预约必须携带属于本预约的接诊签署事件，避免绕过线下接诊直接完成诊疗。</p>
 */
public final class Appointment {
    private final UUID id;
    private final UUID slotId;
    private AppointmentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Appointment(UUID id, UUID slotId, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "预约号不能为空");
        this.slotId = Objects.requireNonNull(slotId, "排班号不能为空");
        this.status = AppointmentStatus.PENDING;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    public static Appointment pending(UUID id, UUID slotId, Clock clock) {
        return new Appointment(id, slotId, now(clock));
    }

    public void confirm(Clock clock) {
        transitionFrom(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, clock);
    }

    public void checkIn(Clock clock) {
        transitionFrom(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN, clock);
    }

    public void startConsultation(Clock clock) {
        transitionFrom(AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS, clock);
    }

    /**
     * 在接诊文书签署后完成预约。
     *
     * @throws IllegalArgumentException 事件来自其他预约
     * @throws IllegalStateException 预约尚未进入接诊中状态
     */
    public void completeAfterEncounterSigned(EncounterSignedEvent event, Clock clock) {
        Objects.requireNonNull(event, "接诊签署事件不能为空");
        if (!id.equals(event.appointmentId())) {
            throw new IllegalArgumentException("接诊签署事件不属于当前预约");
        }
        transitionFrom(AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED, clock);
    }

    /** 仅允许取消尚未签到的待确认或已确认预约。 */
    public void cancel(Clock clock) {
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            throw invalidTransition(AppointmentStatus.CANCELLED);
        }
        status = AppointmentStatus.CANCELLED;
        updatedAt = now(clock);
    }

    public void markNoShow(Clock clock) {
        transitionFrom(AppointmentStatus.CONFIRMED, AppointmentStatus.NO_SHOW, clock);
    }

    private void transitionFrom(AppointmentStatus expected, AppointmentStatus target, Clock clock) {
        if (status != expected) {
            throw invalidTransition(target);
        }
        status = target;
        updatedAt = now(clock);
    }

    private IllegalStateException invalidTransition(AppointmentStatus target) {
        return new IllegalStateException("预约状态 " + status + " 不能转换为 " + target);
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() {
        return id;
    }

    public UUID slotId() {
        return slotId;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
