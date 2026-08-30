package com.community.healthcare.scheduling.application;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 预约写请求使用的规范化幂等键值对象。
 *
 * <p>限制字符集和长度可安全地把键用于唯一约束、日志关联和跨数据库比较，
 * 避免空白或不可见字符造成同一业务请求被视为不同请求。</p>
 */
public record IdempotencyKey(String value) {
    private static final int MAX_LENGTH = 128;
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public IdempotencyKey {
        Objects.requireNonNull(value, "幂等键不能为空");
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || !SAFE_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("幂等键必须为 1-128 位字母、数字或 . _ : -");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}
