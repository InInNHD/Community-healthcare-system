package com.community.healthcare.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MfaAuthenticationTests {
    private static final String BASE32_SECRET = "JBSWY3DPEHPK3PXP";
    private static final Instant NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Test
    void allClinicalRolesBelongToTheStaffPortal() {
        assertThat(AppRole.DOCTOR.portal()).isEqualTo(PortalType.STAFF);
        assertThat(AppRole.NURSE.portal()).isEqualTo(PortalType.STAFF);
        assertThat(AppRole.PHARMACIST.portal()).isEqualTo(PortalType.STAFF);
        assertThat(AppRole.REGISTRAR.portal()).isEqualTo(PortalType.STAFF);
    }

    @Test
    void totpAcceptsTheCurrentCodeAndRejectsAnotherCode() {
        TotpVerifier verifier = new TotpVerifier(Clock.fixed(NOW, ZoneOffset.UTC));
        String code = verifier.currentCode(BASE32_SECRET);

        assertThat(verifier.verify(BASE32_SECRET, code)).isTrue();
        assertThat(verifier.verify(BASE32_SECRET, "000000")).isFalse();
    }

    @Test
    void encryptedMfaSecretDoesNotContainPlaintextAndCanBeRecovered() {
        MfaSecretCipher cipher = new MfaSecretCipher(
                "test-mfa-encryption-key-with-at-least-32-bytes");

        String ciphertext = cipher.encrypt(BASE32_SECRET);

        assertThat(ciphertext).doesNotContain(BASE32_SECRET);
        assertThat(cipher.decrypt(ciphertext)).isEqualTo(BASE32_SECRET);
    }
}
