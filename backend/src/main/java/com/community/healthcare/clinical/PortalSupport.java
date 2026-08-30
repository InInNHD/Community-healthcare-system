package com.community.healthcare.clinical;

import com.community.healthcare.shared.api.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** 从已验证 JWT 中读取门户业务主体和角色声明。 */
final class PortalClaims {
    private PortalClaims() {}

    static Long requiredLong(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value instanceof Number number) return number.longValue();
        throw new AccessDeniedException("账号未关联业务档案");
    }

    static boolean hasRole(Jwt jwt, String role) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(role);
    }
}

/**
 * 一次医护请求的数据范围上下文。
 *
 * <p>医生同时受自己的 doctorId 和站点居民范围约束；护士不绑定 doctorId，但仍受站点范围约束。</p>
 */
record StaffAccessScope(Long staffId, Long staffProfileId, String role, Long appointmentDoctorId, String actor) {
    static StaffAccessScope from(Jwt jwt) {
        Long staffId = PortalClaims.requiredLong(jwt, "staffId");
        String role = PortalClaims.hasRole(jwt, "DOCTOR") ? "DOCTOR" : "NURSE";
        Object profile = jwt.getClaim("staffProfileId");
        Long staffProfileId = profile instanceof Number number ? number.longValue() : null;
        return new StaffAccessScope(staffId, staffProfileId, role,
                "DOCTOR".equals(role) ? staffId : null, jwt.getSubject());
    }
}

/** 集中校验门户分页参数并限制单页最多 100 条。 */
final class PortalPageRequests {
    static final int MAX_PAGE_SIZE = 100;

    private PortalPageRequests() {}

    static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数无效");
        }
        return PageRequest.of(page, size, sort);
    }

    static PageRequest descending(int page, int size, String property) {
        return of(page, size, Sort.by(property).descending());
    }
}

/** 在 DTO 批量映射后保留原始分页元数据。 */
final class PortalPages {
    private PortalPages() {}

    static <S, T> PageResponse<T> from(Page<S> source, List<T> mappedItems) {
        return new PageResponse<>(List.copyOf(mappedItems), source.getTotalElements(), source.getNumber(),
                source.getSize(), source.getTotalPages());
    }
}
