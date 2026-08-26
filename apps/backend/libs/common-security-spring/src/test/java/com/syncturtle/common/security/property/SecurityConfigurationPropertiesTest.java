package com.syncturtle.common.security.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SecurityConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("app.security.csrf.enabled=false");

    @Nested
    class CsrfPropertiesBinding {

        @Test
        void disabledCsrfDoesNotRequireASecret() {
            // arrange + act
            contextRunner.run(context -> {
                // assert
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(CsrfProperties.class).getSigningKey()).isEmpty();
            });
            // verify
        }

    }

    @Nested
    class SecurityCookiePropertiesBinding {

        @Test
        void bindsTheExplicitCookiePolicyWithoutGlobalDomainOrPath() {
            // arrange
            ApplicationContextRunner runner = contextRunner.withPropertyValues(
                    "app.security.cookies.secure=true",
                    "app.security.cookies.managed-prefix-enabled=true",
                    "app.security.cookies.access-same-site=Lax",
                    "app.security.cookies.refresh-same-site=lax",
                    "app.security.cookies.csrf-same-site=LAX",
                    "app.security.cookies.csrf-max-age=45m");
            // conditions
            // act
            runner.run(context -> {
                SecurityCookieProperties properties = context.getBean(SecurityCookieProperties.class);

                // assert
                assertThat(context).hasNotFailed();
                assertThat(properties.isSecure()).isTrue();
                assertThat(properties.isManagedPrefixEnabled()).isTrue();
                assertThat(properties.shouldUseManagedPrefix()).isTrue();
                assertThat(properties.getAccessSameSite()).isEqualTo("Lax");
                assertThat(properties.getRefreshSameSite()).isEqualTo("Lax");
                assertThat(properties.getCsrfSameSite()).isEqualTo("Lax");
                assertThat(properties.getCsrfMaxAge()).isEqualTo(Duration.ofMinutes(45));
            });
            // verify
        }

        @Test
        void rejectsManagedPrefixesWithoutSecureCookies() {
            // arrange
            ApplicationContextRunner runner = contextRunner.withPropertyValues(
                    "app.security.cookies.managed-prefix-enabled=true",
                    "app.security.cookies.secure=false");
            // conditions
            // act + assert
            runner.run(context -> assertThat(context).hasFailed());
            // verify
        }

        @Test
        void rejectsSameSiteNoneWithoutSecureCookies() {
            // arrange
            ApplicationContextRunner runner = contextRunner.withPropertyValues(
                    "app.security.cookies.secure=false",
                    "app.security.cookies.refresh-same-site=None");
            // conditions
            // act + assert
            runner.run(context -> assertThat(context).hasFailed());
            // verify
        }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ CsrfProperties.class, SecurityCookieProperties.class })
    static class TestConfiguration {
    }

}
