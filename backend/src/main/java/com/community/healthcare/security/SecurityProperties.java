package com.community.healthcare.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code app.security} 配置域的强类型映射。
 *
 * <p>记录涵盖 JWT 轮换、密码强度、MFA、演示账号和 CORS；跨字段及环境相关限制由
 * {@link SecurityPropertiesValidator} 在启动期统一校验。</p>
 */
@ConfigurationProperties(prefix = "app.security")
@Validated
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Password password,
        @Valid @NotNull Mfa mfa,
        @Valid @NotNull Bootstrap bootstrap,
        @Valid @NotNull Cors cors) {

    /** JWT 签发方、受众、有效期及轮换密钥集合。 */
    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String audience,
            @NotNull Duration ttl,
            @Valid @NotNull SigningKey activeKey,
            List<@Valid SigningKey> verificationKeys) {
        public Jwt {
            verificationKeys = verificationKeys == null ? List.of() : List.copyOf(verificationKeys);
        }
    }

    /** 对称签名密钥；{@code kid} 用于在轮换集合中选择验证密钥。 */
    public record SigningKey(@NotBlank String kid, @NotBlank String secret) {}

    /** BCrypt 成本和用户密码复杂度策略。 */
    public record Password(
            @Min(10) @Max(14) int bcryptStrength,
            @Min(12) @Max(128) int minLength,
            @Min(32) @Max(256) int maxLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSpecial) {}

    /** 工作人员 MFA 开关、挑战有效期和种子加密密钥。 */
    public record Mfa(
            boolean enabled,
            @NotNull Duration challengeTtl,
            String encryptionKey) {}

    /** 仅供 dev/demo/test 初始化演示账号的配置。 */
    public record Bootstrap(
            boolean enabled,
            boolean mustChangePassword,
            String adminPassword,
            String doctorPassword,
            String nursePassword,
            String residentPassword) {}

    /** 允许携带凭据访问后端的明确来源列表，不接受通配来源。 */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                    .filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
        }
    }
}

/**
 * 在应用接收请求前验证安全配置的组合约束。
 *
 * <p>生产 Profile 会额外禁止演示密钥和固定初始账号，并强制工作人员 MFA，
 * 使危险配置以启动失败而不是运行期降级的方式暴露。</p>
 */
@Component
class SecurityPropertiesValidator implements InitializingBean {
    static final String DEMO_SECRET = "demo-only-active-jwt-key-2026-change-before-production";
    private final SecurityProperties properties;
    private final Environment environment;

    SecurityPropertiesValidator(SecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        validateJwt();
        validateBootstrap();
        validateMfa();
        validateCors();
    }

    private void validateJwt() {
        SecurityProperties.Jwt jwt = properties.jwt();
        if (jwt.ttl().compareTo(Duration.ofMinutes(5)) < 0 || jwt.ttl().compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalStateException("app.security.jwt.ttl 必须在 5 分钟到 24 小时之间");
        }
        if (containsWhitespace(jwt.issuer()) || containsWhitespace(jwt.audience())) {
            throw new IllegalStateException("JWT issuer 和 audience 不能包含空白字符");
        }
        Set<String> kids = new HashSet<>();
        validateKey(jwt.activeKey(), kids);
        for (SecurityProperties.SigningKey key : jwt.verificationKeys()) validateKey(key, kids);

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (jwt.activeKey().secret().equals(DEMO_SECRET)
                    || jwt.activeKey().kid().toLowerCase().startsWith("demo")) {
                throw new IllegalStateException("生产环境禁止使用演示 JWT 密钥，请配置 JWT_ACTIVE_KID/JWT_ACTIVE_SECRET");
            }
            if (properties.bootstrap().enabled()) {
                throw new IllegalStateException("生产环境禁止启用固定演示账号初始化");
            }
        }
    }

    private void validateKey(SecurityProperties.SigningKey key, Set<String> kids) {
        if (!kids.add(key.kid())) throw new IllegalStateException("JWT kid 重复: " + key.kid());
        if (key.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥 " + key.kid() + " 至少需要 32 字节");
        }
    }

    private void validateBootstrap() {
        SecurityProperties.Bootstrap bootstrap = properties.bootstrap();
        if (!bootstrap.enabled()) return;
        if (!environment.acceptsProfiles(Profiles.of("dev", "demo", "test"))) {
            throw new IllegalStateException("固定初始密码只能在显式 dev/demo/test profile 下启用");
        }
        PasswordRules rules = new PasswordRules(properties.password());
        rules.requireStrong(bootstrap.adminPassword());
        rules.requireStrong(bootstrap.doctorPassword());
        rules.requireStrong(bootstrap.nursePassword());
        rules.requireStrong(bootstrap.residentPassword());
    }

    private void validateMfa() {
        SecurityProperties.Mfa mfa = properties.mfa();
        if (mfa.challengeTtl().compareTo(Duration.ofMinutes(1)) < 0
                || mfa.challengeTtl().compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalStateException("MFA 挑战有效期必须在 1 到 10 分钟之间");
        }
        if (mfa.enabled() && (mfa.encryptionKey() == null
                || mfa.encryptionKey().getBytes(StandardCharsets.UTF_8).length < 32)) {
            throw new IllegalStateException("启用 MFA 时 MFA_ENCRYPTION_KEY 至少需要 32 字节");
        }
        if (environment.acceptsProfiles(Profiles.of("prod")) && !mfa.enabled()) {
            throw new IllegalStateException("生产环境必须启用管理员及工作人员 MFA");
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            String bootstrapSecret = environment.getProperty("app.security.mfa.bootstrap-admin-secret", "");
            if (!bootstrapSecret.matches("[A-Z2-7]{16,128}")) {
                throw new IllegalStateException("生产环境必须配置有效的 MFA_BOOTSTRAP_ADMIN_SECRET，避免首个管理员锁死");
            }
        }
    }

    private void validateCors() {
        if (properties.cors().allowedOrigins().stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("启用凭据时 CORS 不允许使用通配来源 *");
        }
    }

    private boolean containsWhitespace(String value) {
        return value.chars().anyMatch(Character::isWhitespace);
    }
}

/**
 * 用户密码的统一强度校验器。
 *
 * <p>除字符规则外还限制 UTF-8 字节数，因为 BCrypt 只处理前 72 字节；
 * 显式拒绝超长输入可避免两个视觉不同的密码得到等价哈希。</p>
 */
final class PasswordRules {
    static final int BCRYPT_MAX_BYTES = 72;
    private final SecurityProperties.Password settings;

    PasswordRules(SecurityProperties.Password settings) {
        this.settings = settings;
    }

    void requireStrong(String password) {
        if (password == null || password.length() < settings.minLength()) {
            throw new IllegalArgumentException("密码长度至少为 " + settings.minLength() + " 位");
        }
        if (password.length() > settings.maxLength()) {
            throw new IllegalArgumentException("密码长度不能超过 " + settings.maxLength() + " 位");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new IllegalArgumentException("BCrypt 密码的 UTF-8 编码不能超过 72 字节");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("密码不能包含空白字符");
        }
        if (settings.requireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("密码必须包含大写字母");
        }
        if (settings.requireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("密码必须包含小写字母");
        }
        if (settings.requireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("密码必须包含数字");
        }
        if (settings.requireSpecial() && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException("密码必须包含特殊字符");
        }
    }
}
