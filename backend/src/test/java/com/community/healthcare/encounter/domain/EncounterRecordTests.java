package com.community.healthcare.encounter.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterRecordTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void draftCanBeEditedAndSigned() {
        UUID appointmentId = UUID.randomUUID();
        EncounterRecord encounter = EncounterRecord.draft(
                UUID.randomUUID(), appointmentId, "初诊记录", CLOCK);

        encounter.editDraftBody("完善后的初诊记录", CLOCK);
        EncounterSignedEvent event = encounter.sign(CLOCK);

        assertThat(encounter.status()).isEqualTo(EncounterStatus.SIGNED);
        assertThat(encounter.body()).isEqualTo("完善后的初诊记录");
        assertThat(event.appointmentId()).isEqualTo(appointmentId);
        assertThat(event.signedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void signedBodyCannotBeChangedInPlace() {
        EncounterRecord encounter = EncounterRecord.draft(
                UUID.randomUUID(), UUID.randomUUID(), "已签署正文", CLOCK);
        encounter.sign(CLOCK);

        assertThatThrownBy(() -> encounter.editDraftBody("试图覆盖", CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SIGNED");
        assertThat(encounter.body()).isEqualTo("已签署正文");
    }

    @Test
    void amendmentKeepsSignedBodyAndCreatesANewVersion() {
        UUID originalId = UUID.randomUUID();
        EncounterRecord original = EncounterRecord.draft(
                originalId, UUID.randomUUID(), "原始正文", CLOCK);
        original.sign(CLOCK);

        EncounterRecord correction = original.amend(
                UUID.randomUUID(), "更正后的正文", "修正血压数值", CLOCK);

        assertThat(original.status()).isEqualTo(EncounterStatus.AMENDED);
        assertThat(original.body()).isEqualTo("原始正文");
        assertThat(correction.status()).isEqualTo(EncounterStatus.DRAFT);
        assertThat(correction.body()).isEqualTo("更正后的正文");
        assertThat(correction.version()).isEqualTo(2);
        assertThat(correction.previousVersionId()).contains(originalId);
        assertThat(correction.amendmentReason()).contains("修正血压数值");
    }

    @Test
    void onlySignedEncounterCanBeAmendedOrVoidedAndTerminalStateIsProtected() {
        EncounterRecord draft = EncounterRecord.draft(
                UUID.randomUUID(), UUID.randomUUID(), "草稿", CLOCK);
        assertThatThrownBy(() -> draft.amend(UUID.randomUUID(), "更正", "原因", CLOCK))
                .isInstanceOf(IllegalStateException.class);

        draft.sign(CLOCK);
        draft.voidRecord("重复建档", CLOCK);

        assertThat(draft.status()).isEqualTo(EncounterStatus.VOID);
        assertThat(draft.voidReason()).contains("重复建档");
        assertThatThrownBy(() -> draft.sign(CLOCK)).isInstanceOf(IllegalStateException.class);
    }
}
