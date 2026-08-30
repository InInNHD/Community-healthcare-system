package com.community.healthcare.publichealth.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicHealthWorkflowTests {
    @Test
    void registryVisitAndAlertTransitionsAreExplicit() {
        assertThat(RegistryStatus.ACTIVE.pause()).isEqualTo(RegistryStatus.PAUSED);
        assertThat(RegistryStatus.PAUSED.activate()).isEqualTo(RegistryStatus.ACTIVE);
        assertThat(RegistryStatus.ACTIVE.exit()).isEqualTo(RegistryStatus.EXITED);
        assertThat(FollowUpVisitStatus.DRAFT.submit()).isEqualTo(FollowUpVisitStatus.SUBMITTED);
        assertThat(FollowUpVisitStatus.SUBMITTED.verify()).isEqualTo(FollowUpVisitStatus.VERIFIED);
        assertThat(FollowUpVisitStatus.SUBMITTED.returnForCorrection()).isEqualTo(FollowUpVisitStatus.RETURNED);
        assertThat(FollowUpVisitStatus.RETURNED.submit()).isEqualTo(FollowUpVisitStatus.SUBMITTED);
        assertThat(HealthAlertStatus.OPEN.acknowledge()).isEqualTo(HealthAlertStatus.ACKNOWLEDGED);
        assertThat(HealthAlertStatus.ACKNOWLEDGED.resolve()).isEqualTo(HealthAlertStatus.RESOLVED);
        assertThat(HealthAlertStatus.OPEN.dismiss()).isEqualTo(HealthAlertStatus.DISMISSED);
        assertThatThrownBy(RegistryStatus.EXITED::activate).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(HealthAlertStatus.RESOLVED::acknowledge).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ruleEvaluationOnlyProducesNonDiagnosticSuggestions() {
        RuleEvaluationResult result = PublicHealthRule.evaluate(
                PriorityPopulationType.HYPERTENSION, "BP_GE_180_110", LocalDate.of(2026, 8, 22));
        assertThat(result.diagnostic()).isFalse();
        assertThat(result.actions()).containsExactlyInAnyOrder("ALERT", "FOLLOW_UP_TASK");
        assertThat(PriorityPopulationType.values()).containsExactly(
                PriorityPopulationType.HYPERTENSION, PriorityPopulationType.T2DM,
                PriorityPopulationType.COPD, PriorityPopulationType.ELDERLY_65_PLUS);
    }
}
