package com.community.healthcare.residentregistry.infrastructure;

import com.community.healthcare.residentregistry.application.RegistryStore;
import com.community.healthcare.residentregistry.application.RegistryViews.*;
import com.community.healthcare.residentregistry.domain.GuardianStatus;
import com.community.healthcare.residentregistry.domain.ProtectedPatientIdentifier;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * 居民主索引存储端口的 JPA/JDBC 适配器。
 *
 * <p>简单实体由 JPA 管理；跨人员、站点和团队的范围查询使用参数化 SQL，
 * 并在查询时同时考虑启用标志与有效期。</p>
 */
@Component
class JpaRegistryStore implements RegistryStore {
    private final PatientIdentifierRepository identifiers;
    private final GuardianRepository guardians;
    private final PatientAccessGrantRepository grants;
    private final JdbcTemplate jdbc;

    JpaRegistryStore(PatientIdentifierRepository identifiers, GuardianRepository guardians,
                     PatientAccessGrantRepository grants, JdbcTemplate jdbc) {
        this.identifiers = identifiers; this.guardians = guardians; this.grants = grants; this.jdbc = jdbc;
    }

    public IdentifierView addIdentifier(long patientId, ProtectedPatientIdentifier value) {
        PatientIdentifierEntity saved = identifiers.save(new PatientIdentifierEntity(patientId, value));
        return identifier(saved);
    }
    public GuardianView createGuardian(long guardianId, long dependentId, String type, String evidence, Instant now) {
        return guardian(guardians.save(new GuardianEntity(guardianId, dependentId, type, evidence, now)));
    }
    public Optional<GuardianView> guardian(long id) { return guardians.findById(id).map(JpaRegistryStore::guardian); }
    public GuardianView updateGuardian(long id, GuardianStatus status, String actor, String evidence, Instant now) {
        GuardianEntity entity = guardians.findById(id).orElseThrow();
        entity.apply(status, actor, evidence, now);
        return guardian(entity);
    }
    public GrantView createGrant(long userId, long patientId, String purpose, String scopeCode,
                                 Instant from, Instant to, String actor, Instant now) {
        return grant(grants.save(new PatientAccessGrantEntity(userId, patientId, purpose, scopeCode, from, to, actor, now)));
    }
    public GrantView revokeGrant(long grantId, Instant now) {
        PatientAccessGrantEntity entity = grants.findById(grantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("访问授权不存在"));
        if (entity.revokedAt != null) throw new IllegalStateException("访问授权已撤销");
        entity.revokedAt = now;
        return grant(entity);
    }
    public Optional<PatientBasicProfile> patient(long patientId) {
        List<PatientBasicProfile> found = jdbc.query("select id, name, gender, birth_date from patient where id = ? and active = true",
                (rs, row) -> new PatientBasicProfile(rs.getLong("id"), rs.getString("name"), rs.getString("gender"),
                        rs.getObject("birth_date", java.time.LocalDate.class), List.of()), patientId);
        if (found.isEmpty()) return Optional.empty();
        List<IdentifierSummary> summaries = identifiers.findByPatientIdAndActiveTrue(patientId).stream()
                .map(value -> new IdentifierSummary(value.identifierType, value.maskedValue)).toList();
        PatientBasicProfile profile = found.get(0);
        return Optional.of(new PatientBasicProfile(profile.id(), profile.name(), profile.gender(), profile.birthDate(), summaries));
    }
    public boolean verifiedGuardian(long guardianId, long dependentId, Instant at) {
        return guardians.existsByGuardianPatientIdAndDependentPatientIdAndStatus(
                guardianId, dependentId, GuardianStatus.VERIFIED);
    }
    public boolean activeGrant(long userId, long patientId, String scope, String purpose, Instant at) {
        return grants.hasActive(userId, patientId, scope, purpose, at);
    }
    public boolean staffScope(long staffId, long patientId, Instant at) {
        Integer count = jdbc.queryForObject("select count(*) from staff_site_assignment ssa join patient_site_enrollment pse "
                + "on pse.site_id = ssa.site_id where ssa.staff_profile_id = ? and pse.patient_id = ? "
                + "and ssa.active = true and pse.active = true and ssa.valid_from <= ? and pse.enrolled_at <= ? "
                + "and (ssa.valid_to is null or ssa.valid_to > ?) and (pse.ended_at is null or pse.ended_at > ?)",
                Integer.class, staffId, patientId, at, at, at, at);
        return count != null && count > 0;
    }
    public Set<Long> scopedPatientIds(long staffId, Instant at) {
        return new LinkedHashSet<>(jdbc.queryForList("select distinct pse.patient_id from staff_site_assignment ssa "
                + "join patient_site_enrollment pse on pse.site_id = ssa.site_id "
                + "where ssa.staff_profile_id = ? and ssa.active = true and pse.active = true "
                + "and ssa.valid_from <= ? and pse.enrolled_at <= ? "
                + "and (ssa.valid_to is null or ssa.valid_to > ?) and (pse.ended_at is null or pse.ended_at > ?)",
                Long.class, staffId, at, at, at, at));
    }
    /**
     * 优先使用 JWT 中的新工作人员档案标识；缺失时按旧 doctor 标识兼容解析。
     *
     * <p>兼容路径只接受活动档案，避免旧标识映射绕过账号状态。</p>
     */
    public Optional<Long> resolveStaffProfileId(Long claimedStaffProfileId, long legacyStaffId) {
        if (claimedStaffProfileId != null) {
            List<Long> claimed = jdbc.queryForList("select id from staff_profile where id = ? and active = true and account_status = 'ACTIVE'",
                    Long.class, claimedStaffProfileId);
            if (!claimed.isEmpty()) return Optional.of(claimed.get(0));
        }
        return jdbc.queryForList("select sp.id from staff_profile sp join doctor d on d.employee_no = sp.staff_no "
                        + "where d.id = ? and sp.active = true and sp.account_status = 'ACTIVE'",
                Long.class, legacyStaffId).stream().findFirst();
    }
    public Optional<Long> patientIdByIdentifierHash(String type, String hash) {
        return identifiers.findFirstByIdentifierTypeAndIdentifierHashAndActiveTrue(type, hash)
                .map(value -> value.patientId);
    }
    private static IdentifierView identifier(PatientIdentifierEntity e) { return new IdentifierView(e.id, e.patientId, e.identifierType, e.identifierHash, e.maskedValue); }
    private static GuardianView guardian(GuardianEntity e) { return new GuardianView(e.id, e.guardianPatientId, e.dependentPatientId, e.relationshipType, e.status, e.evidenceReference); }
    private static GrantView grant(PatientAccessGrantEntity e) { return new GrantView(e.id, e.granteeUserId, e.patientId, e.purpose, e.scopeCode, e.validFrom, e.validTo); }
}

/** 居民受保护标识实体，类型与哈希组合唯一。 */
@Entity @Table(name = "patient_identifier", uniqueConstraints = @UniqueConstraint(name = "uk_patient_identifier_type_hash", columnNames = {"identifier_type", "identifier_hash"}))
class PatientIdentifierEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long patientId;
    @Column(nullable = false, length = 32) String identifierType;
    @Column(nullable = false, length = 64) String identifierHash;
    @Column(nullable = false, length = 128) String maskedValue;
    @Column(nullable = false) boolean active = true;
    @Column(nullable = false, updatable = false) Instant createdAt;
    Instant revokedAt;
    protected PatientIdentifierEntity() {}
    PatientIdentifierEntity(long patientId, ProtectedPatientIdentifier value) { this.patientId = patientId; identifierType = value.type(); identifierHash = value.hash(); maskedValue = value.maskedValue(); createdAt = Instant.now(); }
}

/** 保留申请、核验和撤销审计字段的监护关系实体。 */
@Entity @Table(name = "guardian_relationship")
class GuardianEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long guardianPatientId;
    @Column(nullable = false) Long dependentPatientId;
    @Column(nullable = false, length = 32) String relationshipType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) GuardianStatus status;
    @Column(length = 255) String evidenceReference;
    @Column(nullable = false) Instant requestedAt;
    Instant verifiedAt; @Column(length = 128) String verifiedBy;
    Instant revokedAt; @Column(length = 128) String revokedBy;
    @Column(nullable = false, updatable = false) Instant createdAt;
    @Column(nullable = false) Instant updatedAt;
    @Version long version;
    protected GuardianEntity() {}
    GuardianEntity(long guardianId, long dependentId, String type, String evidence, Instant now) { guardianPatientId = guardianId; dependentPatientId = dependentId; relationshipType = type; evidenceReference = evidence; status = GuardianStatus.PENDING; requestedAt = now; createdAt = now; updatedAt = now; }
    void apply(GuardianStatus target, String actor, String evidence, Instant now) { status = target; evidenceReference = evidence; updatedAt = now; if (target == GuardianStatus.VERIFIED) { verifiedAt = now; verifiedBy = actor; } else if (target == GuardianStatus.REVOKED) { revokedAt = now; revokedBy = actor; } }
}

/** 带用途、范围、有效期和撤销时间的显式访问授权实体。 */
@Entity @Table(name = "patient_access_grant")
class PatientAccessGrantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long granteeUserId;
    @Column(nullable = false) Long patientId;
    @Column(nullable = false, length = 128) String purpose;
    @Column(nullable = false, length = 64) String scopeCode;
    @Column(nullable = false) Instant validFrom;
    Instant validTo; Instant revokedAt;
    @Column(nullable = false, length = 128) String grantedBy;
    @Column(nullable = false, updatable = false) Instant createdAt;
    protected PatientAccessGrantEntity() {}
    PatientAccessGrantEntity(long userId, long patientId, String purpose, String scopeCode, Instant from, Instant to, String actor, Instant now) { granteeUserId = userId; this.patientId = patientId; this.purpose = purpose; this.scopeCode = scopeCode; validFrom = from; validTo = to; grantedBy = actor; createdAt = now; }
}

/** 居民受保护标识内部仓储。 */
interface PatientIdentifierRepository extends JpaRepository<PatientIdentifierEntity, Long> {
    List<PatientIdentifierEntity> findByPatientIdAndActiveTrue(Long patientId);
    Optional<PatientIdentifierEntity> findFirstByIdentifierTypeAndIdentifierHashAndActiveTrue(String type, String hash);
}
/** 监护关系内部仓储。 */
interface GuardianRepository extends JpaRepository<GuardianEntity, Long> { boolean existsByGuardianPatientIdAndDependentPatientIdAndStatus(Long guardianId, Long dependentId, GuardianStatus status); }
/** 显式访问授权内部仓储。 */
interface PatientAccessGrantRepository extends JpaRepository<PatientAccessGrantEntity, Long> {
    @Query("select (count(g) > 0) from PatientAccessGrantEntity g where g.granteeUserId = :userId and g.patientId = :patientId and g.scopeCode = :scope and g.purpose = :purpose and g.revokedAt is null and g.validFrom <= :at and (g.validTo is null or g.validTo > :at)")
    boolean hasActive(@Param("userId") long userId, @Param("patientId") long patientId,
                      @Param("scope") String scope, @Param("purpose") String purpose, @Param("at") Instant at);
}
