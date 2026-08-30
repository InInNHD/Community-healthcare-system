package com.community.healthcare.residentregistry.domain;

/**
 * 居民之间监护关系的状态聚合。
 *
 * <p>本人不能成为自己的监护人；申请仅产生 PENDING 状态，必须由明确操作者核验后才可用于授权，
 * 已核验关系可以撤销但不能重新回到待核验状态。</p>
 */
public final class GuardianRelationship {
    private final long guardianPatientId;
    private final long dependentPatientId;
    private final String relationshipType;
    private GuardianStatus status;

    private GuardianRelationship(long guardianPatientId, long dependentPatientId, String relationshipType) {
        if (guardianPatientId == dependentPatientId) throw new IllegalArgumentException("监护人与被监护人不能相同");
        if (relationshipType == null || relationshipType.isBlank()) throw new IllegalArgumentException("监护关系类型不能为空");
        this.guardianPatientId = guardianPatientId;
        this.dependentPatientId = dependentPatientId;
        this.relationshipType = relationshipType;
        this.status = GuardianStatus.PENDING;
    }

    public static GuardianRelationship pending(long guardianPatientId, long dependentPatientId,
                                                String relationshipType) {
        return new GuardianRelationship(guardianPatientId, dependentPatientId, relationshipType);
    }

    public static GuardianRelationship restore(long guardianPatientId, long dependentPatientId,
                                                String relationshipType, GuardianStatus status) {
        GuardianRelationship relationship = new GuardianRelationship(guardianPatientId, dependentPatientId,
                relationshipType);
        relationship.status = status;
        return relationship;
    }

    /** 由受控工作人员核验待处理关系。 */
    public void verify(String actor) {
        requireActor(actor);
        requireStatus(GuardianStatus.PENDING);
        status = GuardianStatus.VERIFIED;
    }

    /** 撤销已核验关系，使后续访问策略立即失去监护依据。 */
    public void revoke(String actor) {
        requireActor(actor);
        requireStatus(GuardianStatus.VERIFIED);
        status = GuardianStatus.REVOKED;
    }

    private void requireStatus(GuardianStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("当前状态 " + status + " 不允许该操作");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("操作人不能为空");
    }

    public long guardianPatientId() { return guardianPatientId; }
    public long dependentPatientId() { return dependentPatientId; }
    public String relationshipType() { return relationshipType; }
    public GuardianStatus status() { return status; }
}
