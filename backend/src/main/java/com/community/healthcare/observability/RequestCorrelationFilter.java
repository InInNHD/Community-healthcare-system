package com.community.healthcare.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** 为每个 HTTP 请求建立可回传、可检索的关联号，并在请求结束后清理线程上下文。 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-ID";
    private static final String MDC_KEY = "correlationId";
    private static final String VALID = "[A-Za-z0-9._-]{1,64}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied != null && supplied.matches(VALID)
                ? supplied : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** 返回当前请求关联号；非 HTTP 后台任务没有关联号时返回 {@code null}。 */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
