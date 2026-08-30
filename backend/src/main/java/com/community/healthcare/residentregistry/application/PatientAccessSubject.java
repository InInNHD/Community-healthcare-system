package com.community.healthcare.residentregistry.application;

import java.util.Set;

/**
 * 从认证令牌提取的居民数据访问主体。
 *
 * <p>不同门户只会填充与自身相关的业务主体标识；角色集合在构造时复制，防止外部修改授权上下文。</p>
 */
public record PatientAccessSubject(Long userId, Long patientId, Long staffProfileId, Set<String> roles) {
    public PatientAccessSubject {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
