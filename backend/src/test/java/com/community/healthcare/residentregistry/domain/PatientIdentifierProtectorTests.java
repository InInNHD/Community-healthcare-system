package com.community.healthcare.residentregistry.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatientIdentifierProtectorTests {
    private final PatientIdentifierProtector protector =
            new PatientIdentifierProtector("test-pepper-for-stable-hash");

    @Test
    void normalizesEquivalentNationalIdsToTheSameHashWithoutRetainingPlaintext() {
        ProtectedPatientIdentifier first = protector.protect("NATIONAL_ID", " 110105-19491231-002x ");
        ProtectedPatientIdentifier second = protector.protect("national_id", "11010519491231002X");

        assertThat(first.type()).isEqualTo("NATIONAL_ID");
        assertThat(first.hash()).isEqualTo(second.hash()).hasSize(64)
                .doesNotContain("11010519491231002X");
        assertThat(first.maskedValue()).isEqualTo("1101**********002X");
        assertThat(first.toString()).doesNotContain("11010519491231002X");
    }
}
