package com.community.healthcare.publichealth.domain;

/** 重点人群登记状态及其受控迁移规则。 */
public enum RegistryStatus {
    ACTIVE, PAUSED, EXITED;
    public RegistryStatus pause() { return require(ACTIVE, PAUSED); }
    public RegistryStatus activate() { return require(PAUSED, ACTIVE); }
    public RegistryStatus exit() {
        if (this == EXITED) throw invalid();
        return EXITED;
    }
    private RegistryStatus require(RegistryStatus expected, RegistryStatus target) {
        if (this != expected) throw invalid();
        return target;
    }
    private IllegalStateException invalid() { return new IllegalStateException("重点人群登记状态不允许该操作: " + this); }
}
