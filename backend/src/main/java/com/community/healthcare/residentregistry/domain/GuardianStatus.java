package com.community.healthcare.residentregistry.domain;

/** 监护关系从申请、核验到撤销的单向状态。 */
public enum GuardianStatus {
    /** 已申请但尚未授予访问权。 */ PENDING,
    /** 已完成身份核验，可作为访问依据。 */ VERIFIED,
    /** 已撤销，不再授予访问权。 */ REVOKED
}
