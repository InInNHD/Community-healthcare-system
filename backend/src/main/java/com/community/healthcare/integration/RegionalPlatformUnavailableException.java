package com.community.healthcare.integration;

/** 区域平台暂时不可用，出站事件应保留并按策略重试。 */
public class RegionalPlatformUnavailableException extends RuntimeException {
    public RegionalPlatformUnavailableException(String message) { super(message); }
}
