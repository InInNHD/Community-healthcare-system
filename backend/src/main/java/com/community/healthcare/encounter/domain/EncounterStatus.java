package com.community.healthcare.encounter.domain;

/** 接诊文书状态，签署后只能走更正或作废分支。 */
public enum EncounterStatus {
    /** 可编辑但尚未形成正式医疗文书。 */
    DRAFT,
    /** 已签署、不可原位编辑。 */
    SIGNED,
    /** 已由后续更正版本取代，但仍保留用于追溯。 */
    AMENDED,
    /** 已作废并保留作废原因。 */
    VOID
}
