package com.community.healthcare.residentregistry.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrySecurityValidatorTests {
    @Test
    void productionRejectsDefaultOrWeakIdentifierPepper() {
        MockEnvironment prod = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new RegistrySecurityValidator(
                RegistrySecurityValidator.DEFAULT_PEPPER, prod).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("pepper");
        assertThatThrownBy(() -> new RegistrySecurityValidator("short", prod).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("pepper");
        assertThatCode(() -> new RegistrySecurityValidator(
                "prod-identifier-pepper-with-at-least-32-bytes", prod).afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
