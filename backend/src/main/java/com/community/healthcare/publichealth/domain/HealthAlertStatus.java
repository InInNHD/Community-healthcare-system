package com.community.healthcare.publichealth.domain;

/** 健康风险告警的处置状态机。 */
public enum HealthAlertStatus {
    OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED;
    public HealthAlertStatus acknowledge() { return require(OPEN, ACKNOWLEDGED); }
    public HealthAlertStatus resolve() {
        if (this != OPEN && this != ACKNOWLEDGED) throw invalid();
        return RESOLVED;
    }
    public HealthAlertStatus dismiss() {
        if (this != OPEN && this != ACKNOWLEDGED) throw invalid();
        return DISMISSED;
    }
    private HealthAlertStatus require(HealthAlertStatus expected, HealthAlertStatus target) {
        if (this != expected) throw invalid();
        return target;
    }
    private IllegalStateException invalid() { return new IllegalStateException("健康告警状态不允许该操作: " + this); }
}
