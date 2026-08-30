package com.community.healthcare.pharmacy.domain;

/** 处方从医生草稿、药师审方到配药、核对和发药的完整状态。 */
public enum PrescriptionStatus {
    DRAFT,
    SIGNED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    PICKING,
    CHECKED,
    DISPENSED,
    VOID
}
