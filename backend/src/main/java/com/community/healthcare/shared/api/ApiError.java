package com.community.healthcare.shared.api;

import java.time.Instant;
import java.util.Map;

/**
 * 统一的 API 错误响应。
 *
 * @param timestamp 错误响应生成时间
 * @param status HTTP 状态码
 * @param code 供客户端稳定判断错误类型的机器码
 * @param message 面向用户或调用方的错误说明
 * @param fields 字段级校验错误；非校验错误为空映射
 */
public record ApiError(Instant timestamp, int status, String code, String message, Map<String, String> fields) {}
