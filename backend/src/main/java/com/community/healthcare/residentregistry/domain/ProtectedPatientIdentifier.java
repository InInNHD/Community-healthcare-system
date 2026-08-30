package com.community.healthcare.residentregistry.domain;

/**
 * 可安全持久化的居民标识值。
 *
 * @param type 规范化后的标识类型
 * @param hash 用于唯一匹配的 HMAC 哈希
 * @param maskedValue 仅供界面展示的脱敏值
 */
public record ProtectedPatientIdentifier(String type, String hash, String maskedValue) {
}
