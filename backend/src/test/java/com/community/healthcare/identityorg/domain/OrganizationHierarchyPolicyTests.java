package com.community.healthcare.identityorg.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationHierarchyPolicyTests {
    private final OrganizationHierarchyPolicy policy = new OrganizationHierarchyPolicy();

    @Test
    void rejectsSelfParentAndCrossOrganizationSiteDepartment() {
        assertThatThrownBy(() -> policy.requireValidParent(7L, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("上级机构");
        assertThatThrownBy(() -> policy.requireSiteBelongsToOrganization(11L, 12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("站点");
    }

    @Test
    void acceptsRootOrganizationAndMatchingSiteDepartment() {
        assertThatCode(() -> policy.requireValidParent(7L, null)).doesNotThrowAnyException();
        assertThatCode(() -> policy.requireSiteBelongsToOrganization(11L, 11L)).doesNotThrowAnyException();
    }
}
