package com.community.healthcare.pharmacy.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void followsReviewAndDispensingLifecycle() {
        Prescription prescription = validDraft();

        prescription.sign(CLOCK);
        prescription.submitForReview(CLOCK);
        prescription.approve(CLOCK);
        prescription.startPicking(CLOCK);
        prescription.check(CLOCK);
        prescription.dispense(CLOCK);

        assertThat(prescription.status()).isEqualTo(PrescriptionStatus.DISPENSED);
        assertThat(prescription.updatedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void signingRequiresEncounterDiagnosisAndAtLeastOneLine() {
        Prescription noEncounter = Prescription.draft(
                UUID.randomUUID(), null, "高血压", List.of(line()), CLOCK);
        Prescription noDiagnosis = Prescription.draft(
                UUID.randomUUID(), UUID.randomUUID(), " ", List.of(line()), CLOCK);
        Prescription noLines = Prescription.draft(
                UUID.randomUUID(), UUID.randomUUID(), "高血压", List.of(), CLOCK);

        assertThatThrownBy(() -> noEncounter.sign(CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("接诊");
        assertThatThrownBy(() -> noDiagnosis.sign(CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("诊断");
        assertThatThrownBy(() -> noLines.sign(CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("明细");
    }

    @Test
    void rejectionAndVoidAreExplicitAndTerminal() {
        Prescription rejected = validDraft();
        rejected.sign(CLOCK);
        rejected.submitForReview(CLOCK);
        rejected.reject("配伍禁忌", CLOCK);

        assertThat(rejected.status()).isEqualTo(PrescriptionStatus.REJECTED);
        assertThat(rejected.reviewNote()).contains("配伍禁忌");
        assertThatThrownBy(() -> rejected.approve(CLOCK)).isInstanceOf(IllegalStateException.class);

        Prescription voided = validDraft();
        voided.sign(CLOCK);
        voided.voidPrescription("医生撤回", CLOCK);

        assertThat(voided.status()).isEqualTo(PrescriptionStatus.VOID);
        assertThat(voided.voidReason()).contains("医生撤回");
        assertThatThrownBy(() -> voided.submitForReview(CLOCK)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsOutOfOrderTransitionsAndCannotVoidDispensedPrescription() {
        Prescription prescription = validDraft();
        assertThatThrownBy(() -> prescription.approve(CLOCK)).isInstanceOf(IllegalStateException.class);

        prescription.sign(CLOCK);
        prescription.submitForReview(CLOCK);
        prescription.approve(CLOCK);
        prescription.startPicking(CLOCK);
        prescription.check(CLOCK);
        prescription.dispense(CLOCK);

        assertThatThrownBy(() -> prescription.voidPrescription("事后撤回", CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }

    private Prescription validDraft() {
        return Prescription.draft(
                UUID.randomUUID(), UUID.randomUUID(), "原发性高血压", List.of(line()), CLOCK);
    }

    private static PrescriptionLine line() {
        return new PrescriptionLine(UUID.randomUUID(), "苯磺酸氨氯地平片", 14);
    }
}
