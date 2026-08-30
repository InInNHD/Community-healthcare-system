package com.community.healthcare.identityorg.domain;

/**
 * 维护机构、站点和科室之间不能由数据库外键单独表达的层级规则。
 */
public final class OrganizationHierarchyPolicy {
    /** 拒绝把机构自身设为直接上级，避免形成最短组织环。 */
    public void requireValidParent(Long organizationId, Long parentOrganizationId) {
        if (organizationId != null && organizationId.equals(parentOrganizationId)) {
            throw new IllegalArgumentException("机构不能成为自己的上级机构");
        }
    }

    /** 确认科室选择的站点属于同一机构，防止跨机构挂接。 */
    public void requireSiteBelongsToOrganization(long expectedOrganizationId, long actualOrganizationId) {
        if (expectedOrganizationId != actualOrganizationId) {
            throw new IllegalArgumentException("科室所属站点必须属于同一机构");
        }
    }
}
