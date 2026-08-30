package com.community.healthcare.scheduling.domain;

/** 排班时段状态。 */
public enum SlotStatus {
    /** 可被预约。 */ OPEN,
    /** 已由预约占用。 */ RESERVED,
    /** 已完成对应服务。 */ USED,
    /** 预约取消后释放，保留历史而不重新开启同一记录。 */ RELEASED,
    /** 因临时停诊等原因暂停。 */ SUSPENDED
}
