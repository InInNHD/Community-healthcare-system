package com.community.healthcare.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * 统一配置门户认证、JWT 资源服务器、CSRF、CORS 和 URL 级授权。
 *
 * <p>系统使用无状态 JWT，同时把访问令牌放入 HttpOnly Cookie 以降低脚本窃取风险；
 * Cookie 自动随请求发送，因此所有带登录 Cookie 的写请求还必须通过 CSRF 双提交校验。</p>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityModule {
    /** 使用配置化成本创建全系统唯一的 BCrypt 编码器。 */
    @Bean
    PasswordEncoder passwordEncoder(SecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.password().bcryptStrength());
    }

    /**
     * 在非生产演示环境幂等创建管理员账号。
     *
     * <p>仅在账号不存在时创建，重启不会覆盖用户已经修改的密码。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "app.security.bootstrap.enabled", havingValue = "true")
    CommandLineRunner bootstrapAdmin(AppUserRepository users, PasswordEncoder encoder,
                                     SecurityProperties properties) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                SecurityProperties.Bootstrap bootstrap = properties.bootstrap();
                users.save(new AppUser("admin", encoder.encode(bootstrap.adminPassword()), "系统管理员",
                        AppRole.ADMIN, null, null, bootstrap.mustChangePassword()));
            }
        };
    }

    /**
     * 为生产库首个管理员受控写入 MFA 种子，避免启用强制 MFA 后管理员被锁死。
     */
    @Bean
    CommandLineRunner bootstrapProductionAdminMfa(AppUserRepository users, MfaAuthenticationService mfa,
                                                   Environment environment) {
        return args -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) return;
            AppUser admin = users.findByUsername("admin")
                    .orElseThrow(() -> new IllegalStateException("生产库必须先通过受控流程创建 admin 账号"));
            if (!admin.isAccountUsable() || admin.getRole() != AppRole.ADMIN) {
                throw new IllegalStateException("生产库 admin 账号不可用或角色不正确");
            }
            if (admin.getMfaSecretCiphertext() == null) {
                String secret = environment.getRequiredProperty("app.security.mfa.bootstrap-admin-secret");
                admin.enrollMfa(mfa.encryptForProvisioning(secret));
                users.save(admin);
            }
        };
    }

    /** 只允许活动且账号状态可用的用户进入密码认证流程。 */
    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username)
                .filter(AppUser::isActive)
                .filter(AppUser::isAccountUsable)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("用户不存在"));
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** 使用活动密钥签发带 {@code kid} 的 HS256 JWT。 */
    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        SecurityProperties.SigningKey activeKey = properties.jwt().activeKey();
        byte[] secret = activeKey.secret().getBytes(StandardCharsets.UTF_8);
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secret)
                .keyID(activeKey.kid())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.HS256)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    /** 创建支持历史验证密钥、issuer、audience 和账号版本校验的解码器。 */
    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties, AppUserRepository users) {
        return RotatingJwtSupport.decoder(properties, users);
    }

    /**
     * 建立所有 HTTP 请求的最终服务端访问边界。
     *
     * <p>公开端点保持最小集合；管理、医护和居民 API 按路径与角色双重分区。
     * 前端是否展示菜单不影响这里的授权结论。</p>
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.path("/").secure(production).sameSite("Strict"));
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        BearerTokenResolver cookieOrHeader = request -> {
            // 登录和 MFA 验证必须忽略浏览器残留令牌，否则过期 Cookie 会在新认证前触发 401。
            if (AuthenticationBootstrapEndpoint.matches(request)) return null;
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if (AuthCookieService.ACCESS_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                        return cookie.getValue();
                    }
                }
            }
            return headerResolver.resolve(request);
        };
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        .requireCsrfProtectionMatcher(request -> isStateChanging(request.getMethod())
                                && !AuthenticationBootstrapEndpoint.matches(request)
                                && hasAccessCookie(request.getCookies())))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/mfa/verify", "/api/auth/password-policy", "/api/auth/csrf", "/api/public/**", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/me", "/api/auth/change-password", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/resident/**").hasRole("RESIDENT")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST", "REGISTRAR")
                        .requestMatchers("/api/staff/**").hasAnyRole("DOCTOR", "NURSE")
                        .requestMatchers("/api/resident/**").hasRole("RESIDENT")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(cookieOrHeader)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .addFilterBefore(new CookieSessionCsrfFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new MustChangePasswordFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    private static boolean isStateChanging(String method) {
        return !(HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method) || HttpMethod.TRACE.matches(method));
    }

    private static boolean hasAccessCookie(jakarta.servlet.http.Cookie[] cookies) {
        if (cookies == null) return false;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (AuthCookieService.ACCESS_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) return true;
        }
        return false;
    }

    /**
     * 仅允许配置中的明确来源携带 Cookie，并开放前端实际使用的 HTTP 方法。
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}

/**
 * 对使用访问 Cookie 的写请求执行显式 CSRF 双提交校验。
 *
 * <p>Bearer Header 调用不依赖浏览器自动附带凭据，因此不要求 Cookie/Header 配对；
 * 登录和 MFA 验证属于建立会话的引导端点，也由匹配器排除。</p>
 */
final class CookieSessionCsrfFilter extends OncePerRequestFilter {
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    jakarta.servlet.FilterChain filterChain)
            throws jakarta.servlet.ServletException, java.io.IOException {
        if (isStateChanging(request.getMethod())
                && !AuthenticationBootstrapEndpoint.matches(request)
                && cookie(request, AuthCookieService.ACCESS_COOKIE) != null) {
            String cookieToken = cookie(request, CSRF_COOKIE);
            String headerToken = request.getHeader(CSRF_HEADER);
            if (cookieToken == null || headerToken == null || !MessageDigest.isEqual(
                    cookieToken.getBytes(StandardCharsets.UTF_8), headerToken.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "CSRF token invalid");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isStateChanging(String method) {
        return !(HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method) || HttpMethod.TRACE.matches(method));
    }

    private static String cookie(jakarta.servlet.http.HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && !cookie.getValue().isBlank()) return cookie.getValue();
        }
        return null;
    }
}

/**
 * 统一门户账号实体。
 *
 * <p>账号通过 {@code staffId} 或 {@code patientId} 关联业务主体；密码版本和授权版本被写入 JWT，
 * 用于在密码或权限变化后撤销已有令牌。MFA 种子只保存认证加密后的密文。</p>
 */
@Entity
@Table(name = "app_user")
class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String username;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 64)
    private String displayName;
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private AppRole role;
    @Column(name = "staff_id")
    private Long staffId;
    @Column(name = "patient_id")
    private Long patientId;
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;
    @Column(name = "password_version", nullable = false)
    private long passwordVersion;
    @Column(name = "account_status", nullable = false, length = 32)
    private String accountStatus = "ACTIVE";
    @Column(name = "authz_version", nullable = false)
    private long authzVersion;
    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired;
    @Column(name = "mfa_enrolled_at")
    private Instant mfaEnrolledAt;
    @Column(name = "mfa_secret_ciphertext", length = 1024)
    private String mfaSecretCiphertext;
    @Column(name = "staff_profile_id")
    private Long staffProfileId;
    @Column(nullable = false)
    private boolean active = true;
    protected AppUser() {}
    AppUser(String username, String passwordHash, String displayName, AppRole role, Long staffId,
            Long patientId, boolean mustChangePassword) {
        this.username = username; this.passwordHash = passwordHash; this.displayName = displayName;
        this.role = role; this.staffId = staffId; this.patientId = patientId;
        this.mustChangePassword = mustChangePassword;
        this.passwordChangedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.passwordVersion = 0;
    }
    Long getId() { return id; }
    String getUsername() { return username; }
    String getPasswordHash() { return passwordHash; }
    String getDisplayName() { return displayName; }
    AppRole getRole() { return role; }
    Long getStaffId() { return staffId; }
    Long getPatientId() { return patientId; }
    boolean isMustChangePassword() { return mustChangePassword; }
    Instant getPasswordChangedAt() { return passwordChangedAt; }
    long getPasswordVersion() { return passwordVersion; }
    long getAuthzVersion() { return authzVersion; }
    Long getStaffProfileId() { return staffProfileId; }
    boolean isActive() { return active; }
    boolean isAccountUsable() { return active && "ACTIVE".equals(accountStatus); }
    String getMfaSecretCiphertext() { return mfaSecretCiphertext; }
    boolean isMfaRequired() { return mfaRequired; }
    /** 开通 MFA 并提升授权版本，使开通前签发的令牌失效。 */
    void enrollMfa(String encryptedSecret) {
        if (encryptedSecret == null || encryptedSecret.isBlank()) throw new IllegalArgumentException("MFA 密钥不能为空");
        this.mfaSecretCiphertext = encryptedSecret;
        this.mfaRequired = true;
        this.mfaEnrolledAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        incrementAuthzVersion();
    }
    void incrementAuthzVersion() { this.authzVersion = Math.incrementExact(this.authzVersion); }
    void disableAccount() { this.accountStatus = "DISABLED"; }
    /** 更新密码、清除首次改密标记并提升密码版本以撤销旧令牌。 */
    void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
        this.passwordChangedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.passwordVersion = Math.incrementExact(this.passwordVersion);
    }
}

/** 系统角色；每个角色只属于一个门户分区。 */
enum AppRole {
    ADMIN, DOCTOR, NURSE, PHARMACIST, REGISTRAR, RESIDENT;

    PortalType portal() {
        return switch (this) {
            case ADMIN -> PortalType.ADMIN;
            case DOCTOR, NURSE, PHARMACIST, REGISTRAR -> PortalType.STAFF;
            case RESIDENT -> PortalType.RESIDENT;
        };
    }
}

/** 统一入口支持的管理、医护和居民门户。 */
enum PortalType {
    ADMIN, STAFF, RESIDENT;

    static PortalType parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("登录门户无效");
        }
    }
}

/** 安全模块内部使用的账号仓储。 */
interface AppUserRepository extends JpaRepository<AppUser, Long> {
    java.util.Optional<AppUser> findByUsername(String username);
}

/**
 * 校验账号门户归属并签发包含业务主体、安全版本和认证强度声明的 JWT。
 */
@Service
class TokenService {
    private final JwtEncoder encoder;
    private final AppUserRepository users;
    private final SecurityProperties properties;
    TokenService(JwtEncoder encoder, AppUserRepository users, SecurityProperties properties) {
        this.encoder = encoder; this.users = users; this.properties = properties;
    }
    LoginResponse create(Authentication authentication, String requestedPortal) {
        AppUser user = users.findByUsername(authentication.getName()).orElseThrow();
        requirePortal(user, requestedPortal);
        return createFor(user, false);
    }

    /** 防止使用合法账号从不属于其角色的门户登录。 */
    void requirePortal(AppUser user, String requestedPortal) {
        PortalType actualPortal = user.getRole().portal();
        PortalType portal = PortalType.parse(requestedPortal);
        if (portal != null && portal != actualPortal) {
            throw new BadCredentialsException("账号不属于当前登录门户");
        }
    }

    LoginResponse createFor(AppUser user) { return createFor(user, false); }

    /**
     * 为账号签发登录响应。
     *
     * <p>{@code subjectId} 是门户便捷字段：居民取 patientId，工作人员取 staffId；
     * 精确调用仍可使用响应中的独立标识。</p>
     */
    LoginResponse createFor(AppUser user, boolean mfaAssured) {
        PortalType actualPortal = user.getRole().portal();
        Instant now = Instant.now();
        List<String> roles = List.of(user.getRole().name());
        SecurityProperties.Jwt jwt = properties.jwt();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder().issuer(jwt.issuer()).audience(List.of(jwt.audience()))
                .issuedAt(now).expiresAt(now.plus(jwt.ttl()))
                .subject(user.getUsername()).claim("name", user.getDisplayName()).claim("roles", roles)
                .claim("role", user.getRole().name()).claim("portal", actualPortal.name().toLowerCase(Locale.ROOT))
                .claim("mustChangePassword", user.isMustChangePassword())
                .claim("pwdChangedAt", user.getPasswordChangedAt().getEpochSecond())
                .claim("pwdVersion", user.getPasswordVersion())
                .claim("userId", user.getId()).claim("authzVersion", user.getAuthzVersion())
                .claim("amr", mfaAssured ? List.of("pwd", "mfa") : List.of("pwd"))
                .claim("mfa", mfaAssured);
        if (user.getStaffId() != null) claimsBuilder.claim("staffId", user.getStaffId()).claim("subjectId", user.getStaffId());
        if (user.getStaffProfileId() != null) claimsBuilder.claim("staffProfileId", user.getStaffProfileId());
        if (user.getPatientId() != null) claimsBuilder.claim("patientId", user.getPatientId()).claim("subjectId", user.getPatientId());
        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).keyId(jwt.activeKey().kid()).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        Long subjectId = user.getPatientId() != null ? user.getPatientId() : user.getStaffId();
        return new LoginResponse(token, "Bearer", jwt.ttl().toSeconds(), user.getUsername(), user.getDisplayName(), roles,
                actualPortal.name().toLowerCase(Locale.ROOT), subjectId, user.getStaffId(), user.getPatientId(),
                user.isMustChangePassword(), user.getPasswordChangedAt());
    }
}

/** 识别建立新认证会话的端点，供令牌解析和 CSRF 规则共同复用。 */
final class AuthenticationBootstrapEndpoint {
    private AuthenticationBootstrapEndpoint() {}

    static boolean matches(jakarta.servlet.http.HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) return false;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/api/auth/login".equals(path) || "/api/auth/mfa/verify".equals(path);
    }
}

/**
 * 创建和清除浏览器访问 Cookie，并控制生产响应是否回显令牌正文。
 *
 * <p>生产环境仅通过 HttpOnly、Secure、SameSite=Strict Cookie 传递令牌；
 * Demo 环境保留正文令牌以方便接口调试。</p>
 */
@Service
class AuthCookieService {
    static final String ACCESS_COOKIE = "healthcare_access";
    private final boolean production;
    private final SecurityProperties properties;

    AuthCookieService(Environment environment, SecurityProperties properties) {
        this.production = environment.acceptsProfiles(Profiles.of("prod"));
        this.properties = properties;
    }

    ResponseCookie access(String token) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true).secure(production).sameSite("Strict").path("/")
                .maxAge(properties.jwt().ttl()).build();
    }

    ResponseCookie clearAccess() {
        return ResponseCookie.from(ACCESS_COOKIE, "")
                .httpOnly(true).secure(production).sameSite("Strict").path("/").maxAge(0).build();
    }

    LoginResponse browserBody(LoginResponse response) {
        if (!production) return response;
        return new LoginResponse(null, response.tokenType(), response.expiresIn(), response.username(),
                response.displayName(), response.roles(), response.portal(), response.subjectId(), response.staffId(),
                response.patientId(), response.mustChangePassword(), response.passwordChangedAt());
    }
}

/**
 * 统一门户认证、MFA、退出、当前用户和密码策略接口。
 *
 * <p>认证成功响应同时设置访问 Cookie；首次改密限制由服务端过滤器执行，
 * 客户端路由只负责引导用户完成流程。</p>
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthenticationManager authenticationManager;
    private final MfaAuthenticationService mfa;
    private final AccountPasswordService passwords;
    private final SecurityProperties properties;
    private final AuthCookieService cookies;
    AuthController(AuthenticationManager authenticationManager, MfaAuthenticationService mfa,
                   AccountPasswordService passwords, SecurityProperties properties, AuthCookieService cookies) {
        this.authenticationManager = authenticationManager; this.mfa = mfa;
        this.passwords = passwords; this.properties = properties; this.cookies = cookies;
    }
    @PostMapping("/login")
    org.springframework.http.ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(request.username(), request.password()));
        LoginAttempt attempt = mfa.authenticate(authentication, request.portal());
        if (attempt.needsMfa()) {
            return org.springframework.http.ResponseEntity.accepted().body(attempt.challenge());
        }
        return authenticated(attempt.login());
    }

    @PostMapping("/mfa/verify")
    org.springframework.http.ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody MfaVerificationRequest request) {
        return authenticated(mfa.verify(request.challengeToken(), request.code()));
    }
    @GetMapping("/csrf")
    java.util.Map<String, String> csrf(CsrfToken token) {
        return java.util.Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
    @PostMapping("/logout")
    org.springframework.http.ResponseEntity<Void> logout() {
        return org.springframework.http.ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString()).build();
    }
    @GetMapping("/me")
    CurrentUser me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
                   Authentication authentication) {
        return new CurrentUser(authentication.getName(), jwt.getClaimAsString("name"),
                authentication.getAuthorities().stream().map(Object::toString).toList(),
                jwt.getClaimAsString("portal"), numberClaim(jwt, "subjectId"),
                numberClaim(jwt, "staffId"), numberClaim(jwt, "patientId"),
                Boolean.TRUE.equals(jwt.getClaim("mustChangePassword")));
    }

    @RequestMapping(path = "/change-password", method = {RequestMethod.POST, RequestMethod.PUT})
    PasswordChangeResponse changePassword(Authentication authentication,
                                          @Valid @RequestBody PasswordChangeRequest request) {
        return passwords.change(authentication.getName(), request.currentPassword(), request.newPassword());
    }

    @GetMapping("/password-policy")
    PasswordPolicyResponse passwordPolicy() {
        SecurityProperties.Password policy = properties.password();
        return new PasswordPolicyResponse(policy.minLength(), policy.maxLength(), PasswordRules.BCRYPT_MAX_BYTES,
                policy.requireUppercase(), policy.requireLowercase(), policy.requireDigit(), policy.requireSpecial());
    }

    private Long numberClaim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        return value instanceof Number number ? number.longValue() : null;
    }

    private org.springframework.http.ResponseEntity<LoginResponse> authenticated(LoginResponse login) {
        return org.springframework.http.ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(login.accessToken()).toString())
                .body(cookies.browserBody(login));
    }
}

/** 密码登录请求；{@code portal} 用于额外验证账号所属入口。 */
record LoginRequest(@NotBlank String username, @NotBlank String password, String portal) {}
/** MFA 挑战验证请求，验证码固定为六位数字。 */
record MfaVerificationRequest(@NotBlank String challengeToken,
                              @NotBlank @jakarta.validation.constraints.Pattern(regexp = "\\d{6}") String code) {}
/**
 * 登录成功响应，包含角色、门户和业务主体映射。
 *
 * <p>正式环境的 {@code accessToken} 会被移除，令牌只存在于 HttpOnly Cookie。</p>
 */
record LoginResponse(String accessToken, String tokenType, long expiresIn, String username, String displayName,
                     List<String> roles, String portal, Long subjectId, Long staffId, Long patientId,
                     boolean mustChangePassword, Instant passwordChangedAt) {}
/** 从已验证 JWT 投影出的当前会话信息。 */
record CurrentUser(String username, String displayName, List<String> authorities, String portal,
                   Long subjectId, Long staffId, Long patientId, boolean mustChangePassword) {}
/** 当前账号修改密码的输入；最终强度由 {@link PasswordRules} 校验。 */
record PasswordChangeRequest(@NotBlank @Size(max = 256) String currentPassword,
                             @NotBlank @Size(max = 256) String newPassword) {}
/** 前端展示密码要求所需的公开策略，不包含任何密钥或哈希参数。 */
record PasswordPolicyResponse(int minLength, int maxLength, int maxUtf8Bytes, boolean requireUppercase,
                              boolean requireLowercase, boolean requireDigit, boolean requireSpecial) {}
