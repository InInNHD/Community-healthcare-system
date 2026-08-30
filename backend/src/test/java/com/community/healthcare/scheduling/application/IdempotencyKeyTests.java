package com.community.healthcare.scheduling.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTests {
    @Test
    void trimsAndRetainsAValidKey() {
        IdempotencyKey key = IdempotencyKey.of("  appointment-20260822-001  ");

        assertThat(key.value()).isEqualTo("appointment-20260822-001");
    }

    @Test
    void rejectsBlankOversizedAndUnsafeKeys() {
        assertThatThrownBy(() -> IdempotencyKey.of(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdempotencyKey.of("a".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdempotencyKey.of("appointment key"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
