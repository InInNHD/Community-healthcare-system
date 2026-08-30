package com.community.healthcare.clinical;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 既有门户预约状态机。
 *
 * <p>重复提交相同状态视为幂等成功；完成或取消为终态，不能重新开启。</p>
 */
@Component
class AppointmentStatusPolicy {
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            AppointmentStatus.PENDING, EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
            AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED),
            AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));

    /** 校验目标状态是否可由当前状态到达。 */
    void check(AppointmentStatus current, AppointmentStatus target) {
        if (current == null || target == null) throw new IllegalArgumentException("预约状态不能为空");
        if (current == target) return;
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalArgumentException("不允许将预约状态从 " + current + " 变更为 " + target);
        }
    }
}
