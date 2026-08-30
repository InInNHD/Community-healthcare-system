package com.community.healthcare.familydoctor.domain;

/** 签约服务任务从待分派到完成、逾期或异常关闭的状态机。 */
public enum ServiceTaskStatus {
    PENDING_ASSIGNMENT, ASSIGNED, IN_PROGRESS, COMPLETED, OVERDUE, EXCEPTION_CLOSED;

    public ServiceTaskStatus assign() { return require(PENDING_ASSIGNMENT, ASSIGNED); }
    public ServiceTaskStatus start() { return require(ASSIGNED, IN_PROGRESS); }
    public ServiceTaskStatus complete() { return require(IN_PROGRESS, COMPLETED); }
    public ServiceTaskStatus markOverdue() {
        if (this != PENDING_ASSIGNMENT && this != ASSIGNED && this != IN_PROGRESS) throw invalid();
        return OVERDUE;
    }
    public ServiceTaskStatus closeException() {
        if (this != PENDING_ASSIGNMENT && this != ASSIGNED && this != IN_PROGRESS && this != OVERDUE) throw invalid();
        return EXCEPTION_CLOSED;
    }
    private ServiceTaskStatus require(ServiceTaskStatus expected, ServiceTaskStatus target) {
        if (this != expected) throw invalid();
        return target;
    }
    private IllegalStateException invalid() { return new IllegalStateException("服务任务状态不允许该操作: " + this); }
}
