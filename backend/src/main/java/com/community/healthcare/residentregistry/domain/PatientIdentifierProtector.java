package com.community.healthcare.residentregistry.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 将身份证等居民标识转换为 HMAC-SHA256 哈希和脱敏展示值。
 *
 * <p>带 pepper 的确定性哈希既支持精确去重和查找，又避免保存可还原原文；
 * 类型和值先规范化，确保格式差异不会绕过唯一性约束。</p>
 */
public final class PatientIdentifierProtector {
    private final byte[] pepper;

    public PatientIdentifierProtector(String pepper) {
        if (pepper == null || pepper.isBlank()) throw new IllegalArgumentException("标识哈希密钥不能为空");
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    /** 规范化并保护一个居民标识，返回值不含原始输入。 */
    public ProtectedPatientIdentifier protect(String type, String rawValue) {
        String normalizedType = requireText(type, "标识类型").toUpperCase(Locale.ROOT);
        String normalizedValue = normalize(normalizedType, requireText(rawValue, "标识值"));
        return new ProtectedPatientIdentifier(normalizedType, hash(normalizedType + ":" + normalizedValue),
                mask(normalizedValue));
    }

    private String normalize(String type, String value) {
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        if ("NATIONAL_ID".equals(type)) return trimmed.replaceAll("[^0-9X]", "");
        return trimmed.replaceAll("\\s+", "");
    }

    private String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法保护居民标识", ex);
        }
    }

    private String mask(String value) {
        if (value.length() <= 4) return "*".repeat(value.length());
        int visiblePrefix = Math.min(4, value.length() - 2);
        int visibleSuffix = Math.min(4, value.length() - visiblePrefix);
        return value.substring(0, visiblePrefix)
                + "*".repeat(value.length() - visiblePrefix - visibleSuffix)
                + value.substring(value.length() - visibleSuffix);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
        return value;
    }
}
