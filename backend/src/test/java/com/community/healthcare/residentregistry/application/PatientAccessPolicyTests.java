package com.community.healthcare.residentregistry.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatientAccessPolicyTests {
    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private final PatientAccessPolicy policy = new PatientAccessPolicy(
            (guardian, patient, at) -> guardian == 10L && patient == 20L,
            (user, patient, scope, purpose, at) -> user == 30L && patient == 20L
                    && scope.equals("BASIC_PROFILE") && purpose.equals("FAMILY_SUPPORT"),
            (staff, patient, at) -> staff == 40L && patient == 20L,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void allowsSelfVerifiedGuardianExplicitGrantAndScopedRegistrar() {
        assertThat(policy.canRead(new PatientAccessSubject(1L, 20L, null, Set.of("RESIDENT")), 20L)).isTrue();
        assertThat(policy.canRead(new PatientAccessSubject(2L, 10L, null, Set.of("RESIDENT")), 20L)).isTrue();
        assertThat(policy.canRead(new PatientAccessSubject(30L, null, null, Set.of("RESIDENT")), 20L)).isTrue();
        assertThat(policy.canRead(new PatientAccessSubject(4L, null, 40L, Set.of("REGISTRAR")), 20L)).isTrue();
    }

    @Test
    void deniesUnverifiedGuardianAndOutOfScopeStaffButAdminCanReadRegistry() {
        assertThat(policy.canRead(new PatientAccessSubject(2L, 11L, null, Set.of("RESIDENT")), 20L)).isFalse();
        assertThat(policy.canRead(new PatientAccessSubject(4L, null, 41L, Set.of("REGISTRAR")), 20L)).isFalse();
        assertThat(policy.canRead(new PatientAccessSubject(5L, null, null, Set.of("ADMIN")), 20L)).isTrue();
    }

    @Test
    void explicitGrantMustMatchRequestedScopeAndPurpose() {
        PatientAccessSubject granted = new PatientAccessSubject(30L, null, null, Set.of("RESIDENT"));
        assertThat(policy.canRead(granted, 20L, "BASIC_PROFILE", "FAMILY_SUPPORT")).isTrue();
        assertThat(policy.canRead(granted, 20L, "CLINICAL_RECORD", "FAMILY_SUPPORT")).isFalse();
        assertThat(policy.canRead(granted, 20L, "BASIC_PROFILE", "MARKETING")).isFalse();
    }
}
