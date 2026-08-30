package com.community.healthcare.residentregistry.application;

import com.community.healthcare.residentregistry.domain.GuardianStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 居民主索引模块对外暴露的脱敏只读视图集合。 */
public final class RegistryViews {
    private RegistryViews() {}
    /** 受保护居民标识视图，不包含原始证件值。 */
    public record IdentifierView(Long id, Long patientId, String type, String hash, String maskedValue) {}
    /** 监护关系及其核验状态。 */
    public record GuardianView(Long id, Long guardianPatientId, Long dependentPatientId,
                               String relationshipType, GuardianStatus status, String evidenceReference) {}
    /** 指定用户在特定用途和范围下的访问授权。 */
    public record GrantView(Long id, Long granteeUserId, Long patientId, String purpose,
                            String scopeCode, Instant validFrom, Instant validTo) {}
    /** 经访问策略过滤的居民基础档案，只携带脱敏标识摘要。 */
    public record PatientBasicProfile(Long id, String name, String gender, LocalDate birthDate,
                                      List<IdentifierSummary> identifiers) {}
    /** 可安全展示的居民标识摘要。 */
    public record IdentifierSummary(String type, String maskedValue) {}
}
