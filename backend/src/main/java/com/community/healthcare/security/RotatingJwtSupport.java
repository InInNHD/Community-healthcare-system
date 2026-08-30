package com.community.healthcare.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 构建同时信任活动签名密钥和历史验证密钥的 JWT 解码器。
 *
 * <p>新令牌始终由活动密钥签发；轮换期间旧密钥只用于验证，待旧令牌自然过期后即可移除。</p>
 */
final class RotatingJwtSupport {
    private RotatingJwtSupport() {}

    static JwtDecoder decoder(SecurityProperties properties, AppUserRepository users) {
        Map<String, JwtDecoder> decoders = new LinkedHashMap<>();
        addDecoder(decoders, properties.jwt().activeKey(), properties, users);
        properties.jwt().verificationKeys().forEach(key -> addDecoder(decoders, key, properties, users));
        return new KidAwareJwtDecoder(decoders);
    }

    private static void addDecoder(Map<String, JwtDecoder> decoders, SecurityProperties.SigningKey key,
                                   SecurityProperties properties, AppUserRepository users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
                        key.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());
        OAuth2TokenValidator<Jwt> audience = new AudienceValidator(properties.jwt().audience());
        OAuth2TokenValidator<Jwt> passwordVersion = new PasswordVersionValidator(users, properties.mfa().enabled());
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                issuer, audience, passwordVersion));
        decoders.put(key.kid(), decoder);
    }
}

/** 根据 JWT 头部 {@code kid} 选择受信任解码器，拒绝缺失或未知密钥标识。 */
final class KidAwareJwtDecoder implements JwtDecoder {
    private final Map<String, JwtDecoder> decoders;

    KidAwareJwtDecoder(Map<String, JwtDecoder> decoders) {
        this.decoders = Map.copyOf(decoders);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        final String kid;
        try {
            kid = SignedJWT.parse(token).getHeader().getKeyID();
        } catch (ParseException ex) {
            throw new JwtException("JWT 格式无效", ex);
        }
        if (kid == null || kid.isBlank()) throw new JwtException("JWT 缺少 kid");
        JwtDecoder decoder = decoders.get(kid);
        if (decoder == null) throw new JwtException("JWT kid 未被信任");
        return decoder.decode(token);
    }
}

/** 确认令牌受众包含本系统配置的 audience，防止其他系统令牌被误接受。 */
final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error ERROR = new OAuth2Error("invalid_token", "JWT audience 无效", null);
    private final String audience;

    AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return token.getAudience() != null && token.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(ERROR);
    }
}

/**
 * 将无状态 JWT 与账号当前安全版本绑定。
 *
 * <p>密码修改、权限调整或 MFA 开通会提升相应版本，使旧令牌立即失效；
 * 启用工作人员 MFA 时，还会拒绝未携带 MFA 保证的特权令牌。</p>
 */
final class PasswordVersionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error ACCOUNT_ERROR =
            new OAuth2Error("invalid_token", "账号不可用或令牌已因密码修改失效", null);
    private final AppUserRepository users;
    private final boolean privilegedMfaRequired;

    PasswordVersionValidator(AppUserRepository users) {
        this(users, false);
    }

    PasswordVersionValidator(AppUserRepository users, boolean privilegedMfaRequired) {
        this.users = users;
        this.privilegedMfaRequired = privilegedMfaRequired;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getSubject() == null || token.getSubject().isBlank()) {
            return OAuth2TokenValidatorResult.failure(ACCOUNT_ERROR);
        }
        Object claim = token.getClaim("pwdVersion");
        Object authzClaim = token.getClaim("authzVersion");
        if (!(claim instanceof Number tokenVersion) || !(authzClaim instanceof Number authzVersion)) {
            return OAuth2TokenValidatorResult.failure(ACCOUNT_ERROR);
        }
        return users.findByUsername(token.getSubject())
                .filter(AppUser::isAccountUsable)
                .filter(user -> user.getPasswordVersion() == tokenVersion.longValue())
                .filter(user -> user.getAuthzVersion() == authzVersion.longValue())
                .filter(user -> !privilegedMfaRequired || user.getRole() == AppRole.RESIDENT
                        || Boolean.TRUE.equals(token.getClaim("mfa")))
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(ACCOUNT_ERROR));
    }
}
