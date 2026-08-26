package com.syncturtle.platform.gateway.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.syncturtle.platform.gateway.configuration.property.GatewayPassportProperties;
import com.syncturtle.platform.gateway.security.PassportAuthenticationMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GatewayPassportJwtValidatorTest {

    private static final String ISSUER = "https://api.syncturtle.test/auth";
    private static final String AUDIENCE = "syncturtle-test";
    private static final Instant NOW = Instant.now();

    private OAuth2TokenValidator<Jwt> validator;

    @BeforeEach
    void setUp() {
        GatewayPassportProperties properties = new GatewayPassportProperties(
                ISSUER,
                AUDIENCE,
                "X-Auth-");
        PassportAuthenticationMetrics metrics = new PassportAuthenticationMetrics(new SimpleMeterRegistry());
        GatewaySecurityConfiguration configuration = new GatewaySecurityConfiguration(metrics);
        validator = configuration.passportJwtValidator(properties);
    }

    @Nested
    class Validate {

        @Test
        void acceptsExpectedIssuerAudienceTimeAndTokenUse() {
            Jwt jwt = jwt(ISSUER, List.of(AUDIENCE), "access", NOW.minusSeconds(60), NOW.plusSeconds(900));

            OAuth2TokenValidatorResult result = validator.validate(jwt);

            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        void rejectsExpiredJwt() {
            Jwt jwt = jwt(ISSUER, List.of(AUDIENCE), "access", NOW.minusSeconds(900), NOW.minusSeconds(120));

            OAuth2TokenValidatorResult result = validator.validate(jwt);

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        void rejectsWrongIssuer() {
            Jwt jwt = jwt("https://wrong.example/auth", List.of(AUDIENCE), "access", NOW, NOW.plusSeconds(900));

            OAuth2TokenValidatorResult result = validator.validate(jwt);

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        void rejectsWrongAudience() {
            Jwt jwt = jwt(ISSUER, List.of("wrong-audience"), "access", NOW, NOW.plusSeconds(900));

            OAuth2TokenValidatorResult result = validator.validate(jwt);

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        void rejectsWrongTokenUse() {
            Jwt jwt = jwt(ISSUER, List.of(AUDIENCE), "refresh", NOW, NOW.plusSeconds(900));

            OAuth2TokenValidatorResult result = validator.validate(jwt);

            assertThat(result.hasErrors()).isTrue();
        }
    }

    private static Jwt jwt(
            String issuer,
            List<String> audience,
            String tokenUse,
            Instant issuedAt,
            Instant expiresAt) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .subject("c12c7988-b9fe-4299-a72f-e430d004d37b")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_use", tokenUse)
                .build();
    }
}
