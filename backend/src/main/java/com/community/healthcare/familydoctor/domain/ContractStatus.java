package com.community.healthcare.familydoctor.domain;

/** 家庭医生签约从草稿、居民确认到履约结束的状态机。 */
public enum ContractStatus {
    DRAFT, PENDING_CONFIRMATION, ACTIVE, SUSPENDED, EXPIRED, TERMINATED;

    public ContractStatus submit() { return require(DRAFT, PENDING_CONFIRMATION); }
    public ContractStatus confirm() { return require(PENDING_CONFIRMATION, ACTIVE); }
    public ContractStatus suspend() { return require(ACTIVE, SUSPENDED); }
    public ContractStatus expire() { return require(ACTIVE, EXPIRED); }
    public ContractStatus terminate() {
        if (this != ACTIVE && this != SUSPENDED) throw invalid();
        return TERMINATED;
    }
    private ContractStatus require(ContractStatus expected, ContractStatus target) {
        if (this != expected) throw invalid();
        return target;
    }
    private IllegalStateException invalid() { return new IllegalStateException("合同状态不允许该操作: " + this); }
}
