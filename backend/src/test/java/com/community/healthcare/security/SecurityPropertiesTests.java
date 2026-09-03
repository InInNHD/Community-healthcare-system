package com.community.healthcare.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityPropertiesTests {
    @Test
    void rejectsDemoSigningKeyInProduction() {
        Environment environment = profileEnvironment(true);
        SecurityProperties properties = properties("demo-active-2026",
                SecurityPropertiesValidator.DEMO_SECRET, Duration.ofMinutes(30));

        assertThrows(IllegalStateException.class,
                () -> new SecurityPropertiesValidator(properties, environment).afterPropertiesSet());
    }

    @Test
    void rejectsWeakOrDuplicateVerificationKeysAtStartup() {
        Environment environment = profileEnvironment(false);
        SecurityProperties weak = properties("active", "too-short", Duration.ofMinutes(30));
        assertThrows(IllegalStateException.class,
                () -> new SecurityPropertiesValidator(weak, environment).afterPropertiesSet());

        SecurityProperties valid = properties("active",
                "a-secure-active-key-with-at-least-32-randomish-bytes", Duration.ofMinutes(30));
        SecurityProperties duplicate = new SecurityProperties(
                new SecurityProperties.Jwt(valid.jwt().issuer(), valid.jwt().audience(), valid.jwt().ttl(),
                        valid.jwt().activeKey(), List.of(new SecurityProperties.SigningKey(
                        "active", "another-verification-key-with-at-least-32-bytes"))),
                valid.password(), valid.mfa(), valid.bootstrap(), valid.cors());
        assertThrows(IllegalStateException.class,
                () -> new SecurityPropertiesValidator(duplicate, environment).afterPropertiesSet());
    }

    @Test
    void acceptsStrongNonProductionConfiguration() {
        SecurityProperties properties = properties("active-2026",
                "a-secure-active-key-with-at-least-32-randomish-bytes", Duration.ofHours(1));
        assertDoesNotThrow(() -> new SecurityPropertiesValidator(
                properties, profileEnvironment(false)).afterPropertiesSet());
    }

    @Test
    void passwordPolicyRejectsWeakPassword() {
        PasswordRules rules = new PasswordRules(properties("active-2026",
                "a-secure-active-key-with-at-least-32-randomish-bytes", Duration.ofHours(1)).password());
        assertThrows(IllegalArgumentException.class, () -> rules.requireStrong("onlylowercase"));
        assertDoesNotThrow(() -> rules.requireStrong("Strong-password-2026!"));
    }

    @Test
    void passwordPolicyRejectsBcryptInputsOver72Utf8Bytes() {
        PasswordRules rules = new PasswordRules(properties("active-2026",
                "a-secure-active-key-with-at-least-32-randomish-bytes", Duration.ofHours(1)).password());
        assertDoesNotThrow(() -> rules.requireStrong("Ab1!" + "a".repeat(68)));
        assertThrows(IllegalArgumentException.class, () -> rules.requireStrong("Ab1!" + "a".repeat(69)));
        assertThrows(IllegalArgumentException.class, () -> rules.requireStrong("Ab1!" + "测".repeat(23)));
    }

    @Test
    void passwordVersionInvalidatesOldTokenWithoutSecondResolutionRace() {
        AppUser user = new AppUser("resident", "old-hash", "居民", AppRole.RESIDENT, null, 1L, false);
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.findByUsername("resident")).thenReturn(Optional.of(user));
        Jwt token = mock(Jwt.class);
        when(token.getSubject()).thenReturn("resident");
        when(token.getClaim("pwdVersion")).thenReturn(0L);
        when(token.getClaim("authzVersion")).thenReturn(0L);
        PasswordVersionValidator validator = new PasswordVersionValidator(users);

        assertFalse(validator.validate(token).hasErrors());
        user.changePassword("new-hash");
        assertTrue(validator.validate(token).hasErrors());
    }

    @Test
    void accountValidatorRejectsDisabledStaleAuthorizationAndMissingMfaAssurance() {
        AppUser user = new AppUser("doctor", "hash", "医生", AppRole.DOCTOR, 1L, null, false);
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.findByUsername("doctor")).thenReturn(Optional.of(user));
        Jwt token = mock(Jwt.class);
        when(token.getSubject()).thenReturn("doctor");
        when(token.getClaim("pwdVersion")).thenReturn(0L);
        when(token.getClaim("authzVersion")).thenReturn(0L);
        when(token.getClaim("mfa")).thenReturn(false);
        PasswordVersionValidator validator = new PasswordVersionValidator(users, true);

        assertTrue(validator.validate(token).hasErrors());
        when(token.getClaim("mfa")).thenReturn(true);
        assertFalse(validator.validate(token).hasErrors());
        user.incrementAuthzVersion();
        assertTrue(validator.validate(token).hasErrors());
        when(token.getClaim("authzVersion")).thenReturn(1L);
        user.disableAccount();
        assertTrue(validator.validate(token).hasErrors());
    }

    private SecurityProperties properties(String kid, String secret, Duration ttl) {
        return new SecurityProperties(
                new SecurityProperties.Jwt("community-healthcare", "community-healthcare-web", ttl,
                        new SecurityProperties.SigningKey(kid, secret), List.of()),
                new SecurityProperties.Password(12, 12, 128, true, true, true, true),
                new SecurityProperties.Mfa(false, Duration.ofMinutes(5),
                        "test-mfa-encryption-key-with-at-least-32-bytes"),
                new SecurityProperties.Bootstrap(false, true, null, null, null, null, null, null),
                new SecurityProperties.Cors(List.of("http://localhost:*")));
    }

    private Environment profileEnvironment(boolean profileMatches) {
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles(any(org.springframework.core.env.Profiles.class)))
                .thenReturn(profileMatches);
        return environment;
    }
}
