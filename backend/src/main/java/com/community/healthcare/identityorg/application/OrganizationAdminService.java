package com.community.healthcare.identityorg.application;

import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;
import com.community.healthcare.identityorg.application.OrganizationViews.DepartmentView;
import com.community.healthcare.identityorg.application.OrganizationViews.OrganizationView;
import com.community.healthcare.identityorg.application.OrganizationViews.SiteView;
import com.community.healthcare.identityorg.domain.OrganizationHierarchyPolicy;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 编排组织、服务站和科室创建及查询，并为管理写操作追加审计事件。
 *
 * <p>创建科室时同时校验机构和站点归属，防止通过合法但不匹配的标识建立跨机构关系。</p>
 */
@Service
public class OrganizationAdminService {
    private final OrganizationStore store;
    private final AuditTrail audit;
    private final OrganizationHierarchyPolicy hierarchy = new OrganizationHierarchyPolicy();

    public OrganizationAdminService(OrganizationStore store, AuditTrail audit) {
        this.store = store; this.audit = audit;
    }

    /** 创建机构；存在上级机构时先确认其有效性。 */
    @Transactional
    public OrganizationView createOrganization(String code, String name, Long parentId, String actor, String role) {
        if (parentId != null) store.organization(parentId).orElseThrow(() -> new EntityNotFoundException("上级机构不存在"));
        OrganizationView created = store.createOrganization(code.trim(), name.trim(), parentId);
        append(actor, role, "ORGANIZATION_CREATE", "ORGANIZATION", created.id());
        return created;
    }

    /** 在指定机构下创建服务站，并记录管理员操作。 */
    @Transactional
    public SiteView createSite(long organizationId, String code, String name, String type, String address,
                               String actor, String role) {
        store.organization(organizationId).orElseThrow(() -> new EntityNotFoundException("机构不存在"));
        SiteView created = store.createSite(organizationId, code.trim(), name.trim(), type.trim(), address);
        append(actor, role, "SITE_CREATE", "SITE", created.id());
        return created;
    }

    /** 创建科室，并确保所选站点确实属于请求中的机构。 */
    @Transactional
    public DepartmentView createDepartment(long organizationId, long siteId, String code, String name,
                                           String actor, String role) {
        store.organization(organizationId).orElseThrow(() -> new EntityNotFoundException("机构不存在"));
        SiteView site = store.site(siteId).orElseThrow(() -> new EntityNotFoundException("站点不存在"));
        hierarchy.requireSiteBelongsToOrganization(organizationId, site.organizationId());
        DepartmentView created = store.createDepartment(organizationId, siteId, code.trim(), name.trim());
        append(actor, role, "DEPARTMENT_CREATE", "DEPARTMENT", created.id());
        return created;
    }

    @Transactional(readOnly = true) public List<OrganizationView> organizations() { return store.organizations(); }
    @Transactional(readOnly = true) public List<SiteView> sites() { return store.sites(); }
    @Transactional(readOnly = true) public List<DepartmentView> departments() { return store.departments(); }

    private void append(String actor, String role, String action, String type, Long id) {
        audit.append(new AuditEventCommand(actor, role, action, type, id.toString(), "SUCCESS",
                "ADMIN_MANAGEMENT", null, null));
    }
}
