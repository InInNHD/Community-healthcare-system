package com.community.healthcare.residentregistry.application;

import com.community.healthcare.residentregistry.application.RegistryViews.*;
import com.community.healthcare.residentregistry.domain.GuardianStatus;
import com.community.healthcare.residentregistry.domain.ProtectedPatientIdentifier;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * 居民主索引应用层的存储与关系查询端口。
 *
 * <p>除实体读写外，端口集中提供按时间判断的监护、授权和服务范围查询，
 * 使访问策略不依赖数据库实现。</p>
 */
public interface RegistryStore {
    IdentifierView addIdentifier(long patientId, ProtectedPatientIdentifier identifier);
    GuardianView createGuardian(long guardianPatientId, long dependentPatientId, String relationshipType,
                                String evidenceReference, Instant now);
    Optional<GuardianView> guardian(long id);
    GuardianView updateGuardian(long id, GuardianStatus status, String actor, String evidenceReference, Instant now);
    GrantView createGrant(long userId, long patientId, String purpose, String scopeCode,
                          Instant validFrom, Instant validTo, String actor, Instant now);
    GrantView revokeGrant(long grantId, Instant now);
    Optional<PatientBasicProfile> patient(long patientId);
    boolean verifiedGuardian(long guardianPatientId, long dependentPatientId, Instant at);
    boolean activeGrant(long userId, long patientId, String scope, String purpose, Instant at);
    boolean staffScope(long staffProfileId, long patientId, Instant at);
    Set<Long> scopedPatientIds(long staffProfileId, Instant at);
    Optional<Long> resolveStaffProfileId(Long claimedStaffProfileId, long legacyStaffId);
    Optional<Long> patientIdByIdentifierHash(String type, String hash);
}
