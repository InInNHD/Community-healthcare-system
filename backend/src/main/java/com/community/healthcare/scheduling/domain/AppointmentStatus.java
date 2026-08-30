package com.community.healthcare.scheduling.domain;

/** 预约从申请到接诊结束的状态集合。 */
public enum AppointmentStatus {
    /** 居民已提交，等待医护确认。 */ PENDING,
    /** 已确认并保留排班时段。 */ CONFIRMED,
    /** 居民已到站签到。 */ CHECKED_IN,
    /** 已开始线下接诊。 */ IN_PROGRESS,
    /** 接诊文书签署后完成。 */ COMPLETED,
    /** 到诊前取消。 */ CANCELLED,
    /** 已确认但居民未按时到诊。 */ NO_SHOW
}
