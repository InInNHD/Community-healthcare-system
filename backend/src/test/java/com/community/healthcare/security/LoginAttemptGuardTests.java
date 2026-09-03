package com.community.healthcare.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptGuardTests {
    private final LoginAttemptGuard guard = new LoginAttemptGuard(
            Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void locksOneAccountAndSourceAfterFiveFailures() {
        for (int index = 0; index < 5; index++) guard.failed("doctor", "127.0.0.1");

        assertThatThrownBy(() -> guard.check("DOCTOR", "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("稍后再试");
        assertThatCode(() -> guard.check("doctor", "127.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginClearsPreviousFailures() {
        for (int index = 0; index < 4; index++) guard.failed("resident", "127.0.0.1");
        guard.succeeded("resident", "127.0.0.1");
        guard.failed("resident", "127.0.0.1");

        assertThatCode(() -> guard.check("resident", "127.0.0.1")).doesNotThrowAnyException();
    }
}
