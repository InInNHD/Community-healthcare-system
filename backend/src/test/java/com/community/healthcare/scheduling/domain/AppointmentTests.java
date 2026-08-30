package com.community.healthcare.scheduling.domain;

import com.community.healthcare.encounter.domain.EncounterSignedEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void followsTheConsultationLifecycleAndCompletesFromSignedEncounter() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = Appointment.pending(appointmentId, UUID.randomUUID(), CLOCK);

        appointment.confirm(CLOCK);
        appointment.checkIn(CLOCK);
        appointment.startConsultation(CLOCK);
        appointment.completeAfterEncounterSigned(
                new EncounterSignedEvent(UUID.randomUUID(), appointmentId, CLOCK.instant()), CLOCK);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.updatedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void rejectsInvalidTransitionsAndProtectsTerminalAppointments() {
        Appointment appointment = Appointment.pending(UUID.randomUUID(), UUID.randomUUID(), CLOCK);

        assertThatThrownBy(() -> appointment.checkIn(CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");

        appointment.cancel(CLOCK);

        assertThatThrownBy(() -> appointment.confirm(CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void allowsNoShowOnlyAfterConfirmation() {
        Appointment appointment = Appointment.pending(UUID.randomUUID(), UUID.randomUUID(), CLOCK);
        assertThatThrownBy(() -> appointment.markNoShow(CLOCK)).isInstanceOf(IllegalStateException.class);

        appointment.confirm(CLOCK);
        appointment.markNoShow(CLOCK);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void refusesCompletionForAnotherAppointmentEncounter() {
        Appointment appointment = Appointment.pending(UUID.randomUUID(), UUID.randomUUID(), CLOCK);
        appointment.confirm(CLOCK);
        appointment.checkIn(CLOCK);
        appointment.startConsultation(CLOCK);

        EncounterSignedEvent unrelated = new EncounterSignedEvent(
                UUID.randomUUID(), UUID.randomUUID(), CLOCK.instant());

        assertThatThrownBy(() -> appointment.completeAfterEncounterSigned(unrelated, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预约");
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }
}
