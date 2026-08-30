package com.community.healthcare.publichealth.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * 公卫规则的领域评估入口。
 *
 * <p>当前为可替换的最小规则实现，只产出告警和随访任务建议，不直接生成诊断。</p>
 */
public final class PublicHealthRule {
    private PublicHealthRule() {}

    /** 校验规则输入并返回确定性的评估结果。 */
    public static RuleEvaluationResult evaluate(PriorityPopulationType type, String ruleCode, LocalDate date) {
        if (type == null || ruleCode == null || ruleCode.isBlank() || date == null) {
            throw new IllegalArgumentException("规则评估参数不完整");
        }
        return new RuleEvaluationResult(type, ruleCode.trim(), date, false, Set.of("ALERT", "FOLLOW_UP_TASK"));
    }
}
