package com.community.healthcare.publichealth.domain;

/** 随访记录从草稿提交到审核或退回的状态机。 */
public enum FollowUpVisitStatus {
    DRAFT, SUBMITTED, VERIFIED, RETURNED;
    public FollowUpVisitStatus submit() {
        if (this != DRAFT && this != RETURNED) throw new IllegalStateException("当前随访记录状态不允许提交");
        return SUBMITTED;
    }
    public FollowUpVisitStatus verify() { return require(SUBMITTED, VERIFIED); }
    public FollowUpVisitStatus returnForCorrection() { return require(SUBMITTED, RETURNED); }
    private FollowUpVisitStatus require(FollowUpVisitStatus expected, FollowUpVisitStatus target) {
        if (this != expected) throw new IllegalStateException("随访状态不允许该操作: " + this);
        return target;
    }
}
