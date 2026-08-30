package com.community.healthcare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 在服务端强制执行首次登录改密生命周期，避免只依赖前端路由保护。
 *
 * <p>待改密令牌仅可访问登录、MFA、密码策略、当前用户、改密和健康检查等必要端点；
 * 其他受保护业务请求统一返回 {@code PASSWORD_CHANGE_REQUIRED}。</p>
 */
final class MustChangePasswordFilter extends OncePerRequestFilter {
    /** 列出完成改密流程和公开访问所必需的最小豁免端点。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(path)
                || "/api/auth/mfa/verify".equals(path)
                || "/api/auth/password-policy".equals(path)
                || path.startsWith("/api/public/")
                || "/api/auth/me".equals(path)
                || "/api/auth/change-password".equals(path)
                || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication
                && Boolean.TRUE.equals(jwtAuthentication.getToken().getClaim("mustChangePassword"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"timestamp\":\"" + Instant.now()
                    + "\",\"status\":403,\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                    + "\"message\":\"首次登录必须先修改密码\",\"fields\":{}}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
