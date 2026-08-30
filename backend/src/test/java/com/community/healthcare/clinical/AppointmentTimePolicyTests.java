package com.community.healthcare.clinical;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTimePolicyTests {
    private final AppointmentTimePolicy policy = new AppointmentTimePolicy(Clock.fixed(
            Instant.parse("2026-08-22T00:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void validatesAgainstTheConfiguredBusinessZoneInsteadOfTheJvmDefaultZone() {
        assertThatCode(() -> policy.requireFuture(LocalDateTime.of(2026, 8, 22, 8, 30)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireFuture(LocalDateTime.of(2026, 8, 22, 7, 59)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未来");
    }
}
