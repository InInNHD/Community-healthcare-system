package com.community.healthcare.residentregistry.application;

import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;
import com.community.healthcare.residentregistry.application.RegistryViews.*;
import com.community.healthcare.residentregistry.domain.GuardianRelationship;
import com.community.healthcare.residentregistry.domain.GuardianStatus;
import com.community.healthcare.residentregistry.domain.PatientIdentifierProtector;
import com.community.healthcare.residentregistry.domain.ProtectedPatientIdentifier;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 编排居民身份标识、监护关系、访问授权和家庭档案读取。
 *
 * <p>所有管理写操作均追加审计；读取被拒绝时返回“档案不可用”而非暴露目标是否存在，
 * 防止通过差异化错误枚举居民。</p>
 */
@Service
public class RegistryApplicationService {
    private final RegistryStore store;
    private final AuditTrail audit;
    private final Clock clock;
    private final PatientIdentifierProtector identifiers;

    public RegistryApplicationService(RegistryStore store, AuditTrail audit, Clock clock,
                                      @Value("${app.registry.identifier-pepper:demo-r1-identifier-pepper-change-in-production}") String pepper) {
        this.store = store; this.audit = audit; this.clock = clock;
        this.identifiers = new PatientIdentifierProtector(pepper);
    }

    @Transactional
    public IdentifierView addIdentifier(long patientId, String type, String value, String actor, String role) {
        requirePatient(patientId);
        return addProtectedIdentifier(patientId, identifiers.protect(type, value), actor, role);
    }

    /** 将外部标识转换为可匹配哈希和脱敏展示值，不持久化原文。 */
    public ProtectedPatientIdentifier protectIdentifier(String type, String value) {
        return identifiers.protect(type, value);
    }

    /** 通过受保护哈希解析居民标识；空输入直接视为未命中。 */
    public Optional<Long> patientIdByIdentifier(String type, String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return store.patientIdByIdentifierHash(type, identifiers.protect(type, value).hash());
    }

    @Transactional
    public IdentifierView addProtectedIdentifier(long patientId, ProtectedPatientIdentifier identifier,
                                                  String actor, String role) {
        IdentifierView created = store.addIdentifier(patientId, identifier);
        append(actor, role, "PATIENT_IDENTIFIER_CREATE", "PATIENT_IDENTIFIER", created.id(), "IDENTITY_REGISTRATION");
        return created;
    }

    /** 创建待核验监护关系；申请本身不会授予任何数据访问能力。 */
    @Transactional
    public GuardianView requestGuardian(long guardianPatientId, long dependentPatientId, String relationshipType,
                                        String evidenceReference, String actor, String role) {
        requirePatient(guardianPatientId); requirePatient(dependentPatientId);
        GuardianRelationship.pending(guardianPatientId, dependentPatientId, relationshipType);
        GuardianView created = store.createGuardian(guardianPatientId, dependentPatientId,
                relationshipType, evidenceReference, clock.instant());
        append(actor, role, "GUARDIAN_REQUEST", "GUARDIAN_RELATIONSHIP", created.id(), "FAMILY_ACCESS");
        return created;
    }

    /** 核验待处理监护关系并开始授予家庭访问权限。 */
    @Transactional
    public GuardianView verifyGuardian(long id, String evidenceReference, String actor, String role) {
        GuardianView existing = guardian(id);
        GuardianRelationship relationship = GuardianRelationship.restore(existing.guardianPatientId(),
                existing.dependentPatientId(), existing.relationshipType(), existing.status());
        relationship.verify(actor);
        GuardianView updated = store.updateGuardian(id, relationship.status(), actor,
                evidenceReference == null ? existing.evidenceReference() : evidenceReference, clock.instant());
        append(actor, role, "GUARDIAN_VERIFY", "GUARDIAN_RELATIONSHIP", id, "IDENTITY_VERIFICATION");
        return updated;
    }

    @Transactional
    public GuardianView revokeGuardian(long id, String actor, String role) {
        GuardianView existing = guardian(id);
        GuardianRelationship relationship = GuardianRelationship.restore(existing.guardianPatientId(),
                existing.dependentPatientId(), existing.relationshipType(), existing.status());
        relationship.revoke(actor);
        GuardianView updated = store.updateGuardian(id, relationship.status(), actor,
                existing.evidenceReference(), clock.instant());
        append(actor, role, "GUARDIAN_REVOKE", "GUARDIAN_RELATIONSHIP", id, "ACCESS_REVOCATION");
        return updated;
    }

    /** 创建用途受限的临时或长期访问授权。 */
    @Transactional
    public GrantView grant(long userId, long patientId, String purpose, String scopeCode,
                           Instant validFrom, Instant validTo, String actor, String role) {
        requirePatient(patientId);
        if (validTo != null && !validTo.isAfter(validFrom)) throw new IllegalArgumentException("授权结束时间必须晚于开始时间");
        GrantView created = store.createGrant(userId, patientId, purpose, scopeCode, validFrom, validTo, actor, clock.instant());
        append(actor, role, "PATIENT_ACCESS_GRANT_CREATE", "PATIENT_ACCESS_GRANT", created.id(), purpose);
        return created;
    }

    @Transactional
    public GrantView revokeGrant(long grantId, String actor, String role) {
        GrantView revoked = store.revokeGrant(grantId, clock.instant());
        append(actor, role, "PATIENT_ACCESS_GRANT_REVOKE", "PATIENT_ACCESS_GRANT", grantId, "ACCESS_REVOCATION");
        return revoked;
    }

    /**
     * 在统一访问策略和审计保护下读取家庭成员基础档案。
     *
     * @throws EntityNotFoundException 目标不存在或主体无权访问；两种情况故意使用相同响应
     */
    public PatientBasicProfile familyProfile(PatientAccessSubject subject, long targetPatientId) {
        PatientAccessPolicy policy = new PatientAccessPolicy(store::verifiedGuardian, store::activeGrant,
                store::staffScope, clock);
        String actor = subject.userId() != null ? "user:" + subject.userId() : "patient:" + subject.patientId();
        String role = subject.roles().stream().findFirst().orElse("UNKNOWN");
        if (!policy.canRead(subject, targetPatientId, "BASIC_PROFILE", "FAMILY_SUPPORT")) {
            audit.append(new AuditEventCommand(actor, role, "PATIENT_BASIC_PROFILE_READ", "PATIENT",
                    Long.toString(targetPatientId), "DENIED", "FAMILY_SUPPORT", null, null));
            throw new EntityNotFoundException("居民档案不可用");
        }
        PatientBasicProfile profile = requirePatient(targetPatientId);
        audit.append(new AuditEventCommand(actor, role, "PATIENT_BASIC_PROFILE_READ", "PATIENT",
                Long.toString(targetPatientId), "SUCCESS", "FAMILY_SUPPORT", null, null));
        return profile;
    }

    private PatientBasicProfile requirePatient(long id) {
        return store.patient(id).orElseThrow(() -> new EntityNotFoundException("居民档案不可用"));
    }
    private GuardianView guardian(long id) {
        return store.guardian(id).orElseThrow(() -> new EntityNotFoundException("监护关系不存在"));
    }
    private void append(String actor, String role, String action, String type, Long id, String purpose) {
        audit.append(new AuditEventCommand(actor, role, action, type, id.toString(), "SUCCESS", purpose, null, null));
    }
}
