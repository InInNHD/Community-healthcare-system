package com.community.healthcare.identityorg.application;

/** 组织模块对 API 和应用层公开的只读投影视图集合。 */
public final class OrganizationViews {
    private OrganizationViews() {}

    /** 机构视图；{@code parentOrganizationId} 为空表示根机构。 */
    public record OrganizationView(Long id, String code, String name, Long parentOrganizationId) {}
    /** 机构下属服务站视图。 */
    public record SiteView(Long id, Long organizationId, String code, String name, String siteType, String address) {}
    /** 服务站下属科室视图。 */
    public record DepartmentView(Long id, Long organizationId, Long siteId, String code, String name) {}
}
