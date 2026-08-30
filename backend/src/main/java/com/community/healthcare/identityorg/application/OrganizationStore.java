package com.community.healthcare.identityorg.application;

import com.community.healthcare.identityorg.application.OrganizationViews.DepartmentView;
import com.community.healthcare.identityorg.application.OrganizationViews.OrganizationView;
import com.community.healthcare.identityorg.application.OrganizationViews.SiteView;

import java.util.List;
import java.util.Optional;

/**
 * 组织管理应用层使用的存储端口。
 *
 * <p>端口以只读视图传递数据，避免应用层依赖 JPA 实体及其生命周期。</p>
 */
public interface OrganizationStore {
    OrganizationView createOrganization(String code, String name, Long parentOrganizationId);
    SiteView createSite(long organizationId, String code, String name, String siteType, String address);
    DepartmentView createDepartment(long organizationId, long siteId, String code, String name);
    Optional<OrganizationView> organization(long id);
    Optional<SiteView> site(long id);
    List<OrganizationView> organizations();
    List<SiteView> sites();
    List<DepartmentView> departments();
}
