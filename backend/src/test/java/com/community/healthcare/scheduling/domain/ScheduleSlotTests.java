package com.community.healthcare.scheduling.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleSlotTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void followsTheReservedSlotLifecycle() {
        ScheduleSlot slot = ScheduleSlot.open(UUID.randomUUID(), CLOCK);

        slot.reserve(CLOCK);
        assertThat(slot.status()).isEqualTo(SlotStatus.RESERVED);

        slot.markUsed(CLOCK);
        assertThat(slot.status()).isEqualTo(SlotStatus.USED);
        assertThat(slot.updatedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void reservedSlotCanBeReleasedOrSuspended() {
        ScheduleSlot released = ScheduleSlot.open(UUID.randomUUID(), CLOCK);
        released.reserve(CLOCK);
        released.release(CLOCK);

        ScheduleSlot suspended = ScheduleSlot.open(UUID.randomUUID(), CLOCK);
        suspended.reserve(CLOCK);
        suspended.suspend(CLOCK);

        assertThat(released.status()).isEqualTo(SlotStatus.RELEASED);
        assertThat(suspended.status()).isEqualTo(SlotStatus.SUSPENDED);
    }

    @Test
    void rejectsSkippingStatesAndChangingTerminalSlots() {
        ScheduleSlot slot = ScheduleSlot.open(UUID.randomUUID(), CLOCK);

        assertThatThrownBy(() -> slot.markUsed(CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN");

        slot.reserve(CLOCK);
        slot.release(CLOCK);

        assertThatThrownBy(() -> slot.reserve(CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RELEASED");
    }
}
