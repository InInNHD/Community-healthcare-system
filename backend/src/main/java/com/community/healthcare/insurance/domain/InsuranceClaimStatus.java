package com.community.healthcare.insurance.domain;

/** 医保申报从草稿到受理、拒付或结算的状态。 */
public enum InsuranceClaimStatus {
    DRAFT,
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    SETTLED
}
