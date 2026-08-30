package com.community.healthcare.referral.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferralCaseTests {
    @Test
    void followsTheCompleteReferralLifecycleAndProtectsTerminalState() {
        ReferralCase referral = ReferralCase.draft(11L, 21L);

        referral.consent(11L);
        referral.transitionTo(ReferralStatus.SUBMITTED);
        referral.transitionTo(ReferralStatus.ACCEPTED);
        referral.transitionTo(ReferralStatus.SCHEDULED);
        referral.transitionTo(ReferralStatus.ATTENDED);
        referral.transitionTo(ReferralStatus.FEEDBACK_RECEIVED);
        referral.transitionTo(ReferralStatus.DOWN_REFERRED);
        referral.transitionTo(ReferralStatus.CLOSED);

        assertThat(referral.status()).isEqualTo(ReferralStatus.CLOSED);
        assertThatThrownBy(() -> referral.transitionTo(ReferralStatus.ACCEPTED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsSkippedTransitionAndConsentByAnotherResident() {
        ReferralCase referral = ReferralCase.draft(11L, 21L);

        assertThatThrownBy(() -> referral.transitionTo(ReferralStatus.SUBMITTED))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> referral.consent(12L))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void supportsContinueManagementBranchBeforeClosing() {
        ReferralCase referral = ReferralCase.draft(11L, 21L);
        referral.consent(11L);
        referral.transitionTo(ReferralStatus.SUBMITTED);
        referral.transitionTo(ReferralStatus.ACCEPTED);
        referral.transitionTo(ReferralStatus.SCHEDULED);
        referral.transitionTo(ReferralStatus.ATTENDED);
        referral.transitionTo(ReferralStatus.FEEDBACK_RECEIVED);
        referral.transitionTo(ReferralStatus.CONTINUE_MANAGEMENT);
        referral.transitionTo(ReferralStatus.CLOSED);

        assertThat(referral.status()).isEqualTo(ReferralStatus.CLOSED);
    }
}
