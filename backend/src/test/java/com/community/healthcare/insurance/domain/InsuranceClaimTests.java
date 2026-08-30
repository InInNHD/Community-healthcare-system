package com.community.healthcare.insurance.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceClaimTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void simulatedClaimFollowsAcceptedSettlementLifecycle() {
        InsuranceClaim claim = InsuranceClaim.simulated(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("80.00"), CLOCK);

        claim.submit(CLOCK);
        claim.accept("SIM-20260822-001", CLOCK);
        claim.settle(CLOCK);

        assertThat(claim.simulation()).isTrue();
        assertThat(claim.status()).isEqualTo(InsuranceClaimStatus.SETTLED);
        assertThat(claim.externalReference()).contains("SIM-20260822-001");
    }

    @Test
    void rejectedClaimIsTerminalAndInvalidTransitionsAreRefused() {
        InsuranceClaim claim = InsuranceClaim.simulated(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("80.00"), CLOCK);

        assertThatThrownBy(() -> claim.accept("premature", CLOCK))
                .isInstanceOf(IllegalStateException.class);
        claim.submit(CLOCK);
        claim.reject("模拟拒付", CLOCK);

        assertThat(claim.status()).isEqualTo(InsuranceClaimStatus.REJECTED);
        assertThat(claim.rejectionReason()).contains("模拟拒付");
        assertThatThrownBy(() -> claim.settle(CLOCK)).isInstanceOf(IllegalStateException.class);
    }
}
