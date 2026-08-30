package com.community.healthcare.referral.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 转诊单领域对象，集中约束双向转诊的合法状态迁移和居民授权。
 *
 * <p>居民仅能同意本人的转诊；其余状态必须沿预定义路径推进，避免跳过接收、就诊或反馈环节。</p>
 */
public final class ReferralCase {
    private static final Map<ReferralStatus, Set<ReferralStatus>> TRANSITIONS = Map.of(
            ReferralStatus.DRAFT, EnumSet.of(ReferralStatus.CONSENTED),
            ReferralStatus.CONSENTED, EnumSet.of(ReferralStatus.SUBMITTED),
            ReferralStatus.SUBMITTED, EnumSet.of(ReferralStatus.ACCEPTED),
            ReferralStatus.ACCEPTED, EnumSet.of(ReferralStatus.SCHEDULED),
            ReferralStatus.SCHEDULED, EnumSet.of(ReferralStatus.ATTENDED),
            ReferralStatus.ATTENDED, EnumSet.of(ReferralStatus.FEEDBACK_RECEIVED),
            ReferralStatus.FEEDBACK_RECEIVED, EnumSet.of(ReferralStatus.DOWN_REFERRED, ReferralStatus.CONTINUE_MANAGEMENT),
            ReferralStatus.DOWN_REFERRED, EnumSet.of(ReferralStatus.CLOSED),
            ReferralStatus.CONTINUE_MANAGEMENT, EnumSet.of(ReferralStatus.CLOSED));

    private final long patientId;
    private final long createdByStaffId;
    private ReferralStatus status;

    private ReferralCase(long patientId, long createdByStaffId, ReferralStatus status) {
        if (patientId <= 0 || createdByStaffId <= 0) throw new IllegalArgumentException("居民和创建医护不能为空");
        this.patientId = patientId;
        this.createdByStaffId = createdByStaffId;
        this.status = status;
    }

    /** 创建由医护发起的转诊草稿。 */
    public static ReferralCase draft(long patientId, long createdByStaffId) {
        return new ReferralCase(patientId, createdByStaffId, ReferralStatus.DRAFT);
    }

    /** 从持久化状态还原领域对象。 */
    public static ReferralCase restore(long patientId, long createdByStaffId, ReferralStatus status) {
        return new ReferralCase(patientId, createdByStaffId, status);
    }

    /** 由转诊所涉居民本人确认授权。 */
    public void consent(long actorPatientId) {
        if (actorPatientId != patientId) throw new SecurityException("无权同意他人的转诊");
        transitionTo(ReferralStatus.CONSENTED);
    }

    /** 按双向转诊状态图推进到目标状态。 */
    public void transitionTo(ReferralStatus target) {
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new IllegalStateException("转诊状态不能从 " + status + " 变更为 " + target);
        }
        status = target;
    }

    public long patientId() { return patientId; }
    public long createdByStaffId() { return createdByStaffId; }
    public ReferralStatus status() { return status; }
}
