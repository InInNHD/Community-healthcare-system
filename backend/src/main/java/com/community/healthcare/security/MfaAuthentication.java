package com.community.healthcare.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 按 RFC 6238 时间窗口校验六位 TOTP，允许前后各一个窗口以容忍轻微时钟偏差。 */
final class TotpVerifier {
    private static final int TIME_STEP_SECONDS = 30;
    private final Clock clock;

    TotpVerifier(Clock clock) {
        this.clock = clock;
    }

    /** 使用常量时间比较校验验证码，降低基于比较耗时推测结果的风险。 */
    boolean verify(String base32Secret, String submittedCode) {
        if (submittedCode == null || !submittedCode.matches("\\d{6}")) return false;
        long counter = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (constantTimeEquals(code(base32Secret, counter + offset), submittedCode)) return true;
        }
        return false;
    }

    String currentCode(String base32Secret) {
        return code(base32Secret, clock.instant().getEpochSecond() / TIME_STEP_SECONDS);
    }

    private String code(String base32Secret, long counter) {
        try {
            byte[] key = decodeBase32(base32Secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("无法校验 MFA 验证码", ex);
        }
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer result = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int buffer = 0;
        int bits = 0;
        for (char character : normalized.toCharArray()) {
            int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character);
            if (digit < 0) throw new IllegalArgumentException("MFA 密钥格式无效");
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                result.put((byte) ((buffer >> bits) & 0xff));
            }
        }
        byte[] decoded = new byte[result.position()];
        result.flip();
        result.get(decoded);
        return decoded;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }
}

/**
 * 使用 AES-GCM 加密数据库中的 MFA 种子。
 *
 * <p>密文携带版本前缀和随机 nonce，便于未来轮换格式并避免相同种子产生相同密文。</p>
 */
final class MfaSecretCipher {
    private static final String PREFIX = "v1:";
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    MfaSecretCipher(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("MFA 加密密钥至少需要 32 字节");
        }
        try {
            this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                    .digest(encryptionKey.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("无法初始化 MFA 密钥保护", ex);
        }
    }

    /** 加密 Base32 种子；返回值可安全持久化，但仍应按敏感数据保护。 */
    String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("无法保护 MFA 密钥", ex);
        }
    }

    /** 解密当前版本密文，并拒绝未知版本或认证标签不匹配的数据。 */
    String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            throw new IllegalStateException("MFA 密钥密文版本无效");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(ciphertext.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[12];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("无法解密 MFA 密钥", ex);
        }
    }
}

/**
 * 编排密码认证后的 MFA 挑战和验证。
 *
 * <p>挑战只保存在当前进程内、具有短有效期且验证时先删除，因此同一挑战不能重放。
 * 多实例部署时应将该临时状态替换为共享且具备原子消费能力的存储。</p>
 */
@Service
class MfaAuthenticationService {
    private final AppUserRepository users;
    private final TokenService tokens;
    private final SecurityProperties properties;
    private final TotpVerifier totp;
    private final MfaSecretCipher cipher;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingChallenge> challenges = new ConcurrentHashMap<>();

    MfaAuthenticationService(AppUserRepository users, TokenService tokens, SecurityProperties properties, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.properties = properties;
        this.clock = clock;
        this.totp = new TotpVerifier(clock);
        this.cipher = new MfaSecretCipher(properties.mfa().encryptionKey());
    }

    /**
     * 校验账号所属门户，并根据角色和配置决定直接签发令牌或返回 MFA 挑战。
     */
    LoginAttempt authenticate(Authentication authentication, String requestedPortal) {
        AppUser user = users.findByUsername(authentication.getName()).orElseThrow();
        tokens.requirePortal(user, requestedPortal);
        if (!properties.mfa().enabled() || user.getRole() == AppRole.RESIDENT) {
            return LoginAttempt.authenticated(tokens.createFor(user, false));
        }
        if (user.getMfaSecretCiphertext() == null) {
            throw new BadCredentialsException("该账号尚未完成 MFA 配置，请联系管理员");
        }
        Instant now = clock.instant();
        challenges.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(properties.mfa().challengeTtl());
        challenges.put(token, new PendingChallenge(user.getUsername(), expiresAt));
        return LoginAttempt.challenge(new MfaChallengeResponse(true, token,
                properties.mfa().challengeTtl().toSeconds()));
    }

    /**
     * 一次性消费 MFA 挑战并签发带 {@code mfa=true} 保证的登录响应。
     *
     * <p>先移除再校验可阻止攻击者对同一挑战反复猜测验证码。</p>
     */
    LoginResponse verify(String challengeToken, String code) {
        PendingChallenge challenge = challenges.remove(challengeToken);
        if (challenge == null || !challenge.expiresAt().isAfter(clock.instant())) {
            throw new BadCredentialsException("MFA 挑战已失效，请重新登录");
        }
        AppUser user = users.findByUsername(challenge.username()).filter(AppUser::isAccountUsable)
                .orElseThrow(() -> new BadCredentialsException("账号不可用"));
        if (!totp.verify(cipher.decrypt(user.getMfaSecretCiphertext()), code)) {
            throw new BadCredentialsException("MFA 验证码错误");
        }
        return tokens.createFor(user, true);
    }

    String encryptForProvisioning(String base32Secret) {
        return cipher.encrypt(base32Secret);
    }

    String currentCodeForTesting(String base32Secret) {
        return totp.currentCode(base32Secret);
    }

    private record PendingChallenge(String username, Instant expiresAt) {}
}

/** 密码认证阶段的二选一结果：完成登录或要求继续 MFA。 */
record LoginAttempt(LoginResponse login, MfaChallengeResponse challenge) {
    static LoginAttempt authenticated(LoginResponse login) { return new LoginAttempt(login, null); }
    static LoginAttempt challenge(MfaChallengeResponse challenge) { return new LoginAttempt(null, challenge); }
    boolean needsMfa() { return challenge != null; }
}

/** 返回给客户端的短期 MFA 挑战，{@code expiresIn} 单位为秒。 */
record MfaChallengeResponse(boolean mfaRequired, String challengeToken, long expiresIn) {}
