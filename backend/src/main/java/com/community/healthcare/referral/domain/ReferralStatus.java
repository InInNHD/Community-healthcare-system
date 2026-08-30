package com.community.healthcare.referral.domain;

/** 双向转诊从创建、居民同意、上转就诊到下转随访及闭环的状态。 */
public enum ReferralStatus {
    DRAFT, CONSENTED, SUBMITTED, ACCEPTED, SCHEDULED, ATTENDED,
    FEEDBACK_RECEIVED, DOWN_REFERRED, CONTINUE_MANAGEMENT, CLOSED
}
