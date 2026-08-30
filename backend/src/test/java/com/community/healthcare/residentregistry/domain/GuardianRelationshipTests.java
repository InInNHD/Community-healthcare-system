package com.community.healthcare.residentregistry.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardianRelationshipTests {
    @Test
    void followsPendingVerifiedRevokedStateMachine() {
        GuardianRelationship relationship = GuardianRelationship.pending(1L, 2L, "PARENT");

        relationship.verify("admin");
        assertThat(relationship.status()).isEqualTo(GuardianStatus.VERIFIED);
        relationship.revoke("admin");
        assertThat(relationship.status()).isEqualTo(GuardianStatus.REVOKED);
    }

    @Test
    void rejectsSkippingOrRepeatingTransitions() {
        GuardianRelationship pending = GuardianRelationship.pending(1L, 2L, "PARENT");
        assertThatThrownBy(() -> pending.revoke("admin"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDING");

        pending.verify("admin");
        assertThatThrownBy(() -> pending.verify("admin"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("VERIFIED");
        pending.revoke("admin");
        assertThatThrownBy(() -> pending.verify("admin"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("REVOKED");
    }
}
