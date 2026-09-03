package com.community.healthcare.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 对“账号 + 来源地址”执行有界登录失败限流，降低密码喷洒和暴力破解风险。
 *
 * <p>单中心首期部署不额外引入 Redis；状态仅影响当前进程，未来扩展为多实例时可在保持
 * 控制器调用方式不变的前提下替换为共享存储。</p>
 */
@Service
class LoginAttemptGuard {
    private static final int MAX_FAILURES = 5;
    private static final int MAX_TRACKED_KEYS = 20_000;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    LoginAttemptGuard(Clock clock) {
        this.clock = clock;
    }

    void check(String username, String remoteAddress) {
        AttemptWindow current = attempts.get(key(username, remoteAddress));
        if (current != null && current.lockedUntil() != null
                && current.lockedUntil().isAfter(clock.instant())) {
            throw new BadCredentialsException("登录尝试过于频繁，请稍后再试");
        }
    }

    void failed(String username, String remoteAddress) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_KEYS) purgeExpired(now);
        if (attempts.size() >= MAX_TRACKED_KEYS) return;
        attempts.compute(key(username, remoteAddress), (ignored, previous) -> {
            int failures = previous == null || previous.startedAt().plus(WINDOW).isBefore(now)
                    ? 1 : previous.failures() + 1;
            Instant startedAt = failures == 1 ? now : previous.startedAt();
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new AttemptWindow(failures, startedAt, lockedUntil);
        });
    }

    void succeeded(String username, String remoteAddress) {
        attempts.remove(key(username, remoteAddress));
    }

    private void purgeExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> {
            AttemptWindow value = entry.getValue();
            Instant expiresAt = value.lockedUntil() == null
                    ? value.startedAt().plus(WINDOW) : value.lockedUntil();
            return !expiresAt.isAfter(now);
        });
    }

    private String key(String username, String remoteAddress) {
        String account = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String source = remoteAddress == null ? "unknown" : remoteAddress;
        return account + '|' + source;
    }

    private record AttemptWindow(int failures, Instant startedAt, Instant lockedUntil) {}
}
