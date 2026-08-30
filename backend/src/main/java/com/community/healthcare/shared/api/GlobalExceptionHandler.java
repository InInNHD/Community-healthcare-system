package com.community.healthcare.shared.api;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将跨模块常见异常转换为稳定的 HTTP 错误契约。
 *
 * <p>处理器避免向客户端泄露数据库或框架异常细节；具体模块若需要更精确的业务错误码，
 * 应在模块内先转换为明确的业务异常。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "提交的数据不符合要求", fields);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> notFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "数据已存在或仍被其他记录引用", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, fields));
    }
}
