package com.community.healthcare.residentregistry.application;

import java.time.Clock;
import java.time.Instant;

/**
 * 集中判断某个认证主体能否读取目标居民数据。
 *
 * <p>管理员、本居民本人、已核验监护人、有效显式授权以及处于服务范围内的登记人员可访问；
 * 规则默认拒绝，且通过端口查询关系状态，避免调用方自行拼接权限条件。</p>
 */
public final class PatientAccessPolicy {
    @FunctionalInterface
    /** 查询指定时刻有效的监护关系。 */
    public interface GuardianAccess {
        boolean isVerified(long guardianPatientId, long patientId, Instant at);
    }

    @FunctionalInterface
    /** 查询同时匹配范围、用途和有效期的显式授权。 */
    public interface ExplicitGrantAccess {
        boolean hasGrant(long userId, long patientId, String scope, String purpose, Instant at);
    }

    @FunctionalInterface
    /** 查询工作人员与居民是否处于同一有效服务范围。 */
    public interface StaffScopeAccess {
        boolean isInScope(long staffProfileId, long patientId, Instant at);
    }

    private final GuardianAccess guardians;
    private final ExplicitGrantAccess grants;
    private final StaffScopeAccess staffScopes;
    private final Clock clock;

    public PatientAccessPolicy(GuardianAccess guardians, ExplicitGrantAccess grants,
                               StaffScopeAccess staffScopes, Clock clock) {
        this.guardians = guardians;
        this.grants = grants;
        this.staffScopes = staffScopes;
        this.clock = clock;
    }

    public boolean canRead(PatientAccessSubject subject, long targetPatientId) {
        return canRead(subject, targetPatientId, "BASIC_PROFILE", "FAMILY_SUPPORT");
    }

    /**
     * 按从强身份到委托关系的顺序判断访问许可。
     *
     * <p>工作人员访问目前只允许 REGISTRAR 走通用策略；医护的临床范围由专门查询服务进一步收窄。</p>
     */
    public boolean canRead(PatientAccessSubject subject, long targetPatientId, String scope, String purpose) {
        Instant now = clock.instant();
        if (subject.roles().contains("ADMIN")) return true;
        if (subject.patientId() != null && subject.patientId() == targetPatientId) return true;
        if (subject.patientId() != null && guardians.isVerified(subject.patientId(), targetPatientId, now)) return true;
        if (subject.userId() != null && grants.hasGrant(subject.userId(), targetPatientId, scope, purpose, now)) return true;
        return subject.roles().contains("REGISTRAR") && subject.staffProfileId() != null
                && staffScopes.isInScope(subject.staffProfileId(), targetPatientId, now);
    }
}
