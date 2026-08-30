package com.community.healthcare.familydoctor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FamilyDoctorWorkflowTests {
    @Test
    void contractFollowsConfirmationLifecycleAndProtectsTerminalStates() {
        assertThat(ContractStatus.DRAFT.submit()).isEqualTo(ContractStatus.PENDING_CONFIRMATION);
        assertThat(ContractStatus.PENDING_CONFIRMATION.confirm()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(ContractStatus.ACTIVE.suspend()).isEqualTo(ContractStatus.SUSPENDED);
        assertThat(ContractStatus.ACTIVE.expire()).isEqualTo(ContractStatus.EXPIRED);
        assertThat(ContractStatus.SUSPENDED.terminate()).isEqualTo(ContractStatus.TERMINATED);
        assertThatThrownBy(ContractStatus.TERMINATED::confirm).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(ContractStatus.EXPIRED::suspend).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void serviceTaskUsesAssignmentLifecycleAndTerminalProtection() {
        assertThat(ServiceTaskStatus.PENDING_ASSIGNMENT.assign()).isEqualTo(ServiceTaskStatus.ASSIGNED);
        assertThat(ServiceTaskStatus.ASSIGNED.start()).isEqualTo(ServiceTaskStatus.IN_PROGRESS);
        assertThat(ServiceTaskStatus.IN_PROGRESS.complete()).isEqualTo(ServiceTaskStatus.COMPLETED);
        assertThat(ServiceTaskStatus.ASSIGNED.markOverdue()).isEqualTo(ServiceTaskStatus.OVERDUE);
        assertThat(ServiceTaskStatus.IN_PROGRESS.closeException()).isEqualTo(ServiceTaskStatus.EXCEPTION_CLOSED);
        assertThatThrownBy(ServiceTaskStatus.COMPLETED::start).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(ServiceTaskStatus.OVERDUE::complete).isInstanceOf(IllegalStateException.class);
    }
}
