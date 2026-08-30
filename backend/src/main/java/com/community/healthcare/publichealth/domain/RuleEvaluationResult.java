package com.community.healthcare.publichealth.domain;

import java.time.LocalDate;
import java.util.Set;

/** 公卫规则评估结果；{@code diagnostic=false} 强调结果不构成临床诊断。 */
public record RuleEvaluationResult(PriorityPopulationType populationType, String ruleCode,
                                   LocalDate evaluatedOn, boolean diagnostic, Set<String> actions) {
}
