package com.community.healthcare.scheduling.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 单个可预约排班时段的状态聚合。
 *
 * <p>预约成功后时段由开放转为已保留；接诊使用、取消释放或暂停均只能从已保留状态发生，
 * 以防同一时段被重复消费。</p>
 */
public final class ScheduleSlot {
    private final UUID id;
    private SlotStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private ScheduleSlot(UUID id, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "排班号不能为空");
        this.status = SlotStatus.OPEN;
        this.createdAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    public static ScheduleSlot open(UUID id, Clock clock) {
        return new ScheduleSlot(id, now(clock));
    }

    /** 原子业务语义上的占用；持久化层仍需通过锁或唯一约束处理并发。 */
    public void reserve(Clock clock) {
        transitionFrom(SlotStatus.OPEN, SlotStatus.RESERVED, clock);
    }

    public void markUsed(Clock clock) {
        transitionFrom(SlotStatus.RESERVED, SlotStatus.USED, clock);
    }

    public void release(Clock clock) {
        transitionFrom(SlotStatus.RESERVED, SlotStatus.RELEASED, clock);
    }

    public void suspend(Clock clock) {
        transitionFrom(SlotStatus.RESERVED, SlotStatus.SUSPENDED, clock);
    }

    private void transitionFrom(SlotStatus expected, SlotStatus target, Clock clock) {
        if (status != expected) {
            throw new IllegalStateException("排班状态 " + status + " 不能转换为 " + target);
        }
        status = target;
        updatedAt = now(clock);
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() {
        return id;
    }

    public SlotStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
