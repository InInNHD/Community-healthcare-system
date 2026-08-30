package com.community.healthcare.identityorg.infrastructure;

import com.community.healthcare.identityorg.application.OrganizationStore;
import com.community.healthcare.identityorg.application.OrganizationViews.DepartmentView;
import com.community.healthcare.identityorg.application.OrganizationViews.OrganizationView;
import com.community.healthcare.identityorg.application.OrganizationViews.SiteView;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 将组织存储端口映射到内部 JPA 实体和仓储。 */
@Component
class JpaOrganizationStore implements OrganizationStore {
    private final OrganizationRepository organizations;
    private final SiteRepository sites;
    private final DepartmentRepository departments;

    JpaOrganizationStore(OrganizationRepository organizations, SiteRepository sites,
                         DepartmentRepository departments) {
        this.organizations = organizations; this.sites = sites; this.departments = departments;
    }

    public OrganizationView createOrganization(String code, String name, Long parentId) {
        return view(organizations.save(new OrganizationEntity(code, name, parentId)));
    }
    public SiteView createSite(long organizationId, String code, String name, String type, String address) {
        return view(sites.save(new SiteEntity(organizationId, code, name, type, address)));
    }
    public DepartmentView createDepartment(long organizationId, long siteId, String code, String name) {
        return view(departments.save(new DepartmentEntity(organizationId, siteId, code, name)));
    }
    public Optional<OrganizationView> organization(long id) { return organizations.findById(id).map(JpaOrganizationStore::view); }
    public Optional<SiteView> site(long id) { return sites.findById(id).map(JpaOrganizationStore::view); }
    public List<OrganizationView> organizations() { return organizations.findAll().stream().map(JpaOrganizationStore::view).toList(); }
    public List<SiteView> sites() { return sites.findAll().stream().map(JpaOrganizationStore::view).toList(); }
    public List<DepartmentView> departments() { return departments.findAll().stream().map(JpaOrganizationStore::view).toList(); }

    private static OrganizationView view(OrganizationEntity e) { return new OrganizationView(e.id, e.code, e.name, e.parentOrganizationId); }
    private static SiteView view(SiteEntity e) { return new SiteView(e.id, e.organizationId, e.code, e.name, e.siteType, e.address); }
    private static DepartmentView view(DepartmentEntity e) { return new DepartmentView(e.id, e.organizationId, e.siteId, e.code, e.name); }
}

/** 组织类实体共享的启用状态、审计时间和乐观锁字段。 */
@MappedSuperclass
abstract class OrgBaseEntity {
    @Column(nullable = false) boolean active = true;
    @Column(nullable = false, updatable = false) Instant createdAt;
    @Column(nullable = false) Instant updatedAt;
    @Version long version;
    @PrePersist void create() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}

/** 社区医疗机构持久化实体。 */
@Entity @Table(name = "organization")
class OrganizationEntity extends OrgBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true, length = 64) String code;
    @Column(nullable = false, length = 128) String name;
    Long parentOrganizationId;
    protected OrganizationEntity() {}
    OrganizationEntity(String code, String name, Long parentId) { this.code = code; this.name = name; this.parentOrganizationId = parentId; }
}

/** 机构下服务站实体，站点编码在机构内唯一。 */
@Entity @Table(name = "site", uniqueConstraints = @UniqueConstraint(name = "uk_site_org_code", columnNames = {"organization_id", "code"}))
class SiteEntity extends OrgBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long organizationId;
    @Column(nullable = false, length = 64) String code;
    @Column(nullable = false, length = 128) String name;
    @Column(nullable = false, length = 32) String siteType;
    @Column(length = 255) String address;
    protected SiteEntity() {}
    SiteEntity(long organizationId, String code, String name, String type, String address) { this.organizationId = organizationId; this.code = code; this.name = name; this.siteType = type; this.address = address; }
}

/** 服务站下科室实体，科室编码在站点内唯一。 */
@Entity @Table(name = "department", uniqueConstraints = @UniqueConstraint(name = "uk_department_site_code", columnNames = {"site_id", "code"}))
class DepartmentEntity extends OrgBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long organizationId;
    @Column(nullable = false) Long siteId;
    @Column(nullable = false, length = 64) String code;
    @Column(nullable = false, length = 128) String name;
    protected DepartmentEntity() {}
    DepartmentEntity(long organizationId, long siteId, String code, String name) { this.organizationId = organizationId; this.siteId = siteId; this.code = code; this.name = name; }
}

/** 机构内部仓储。 */
interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {}
/** 服务站内部仓储。 */
interface SiteRepository extends JpaRepository<SiteEntity, Long> {}
/** 科室内部仓储。 */
interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {}
