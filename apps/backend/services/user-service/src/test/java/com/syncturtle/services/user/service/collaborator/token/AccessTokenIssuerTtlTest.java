package com.syncturtle.services.user.service.collaborator.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.configuration.property.PassportProperties;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;

class AccessTokenIssuerTtlTest {

    @Nested
    class IssueAccessToken {

        @Test
        void usesConfiguredFifteenMinuteLifetime() {
            PassportProperties passport = new PassportProperties(
                    "https://api.syncturtle.test/auth",
                    "syncturtle-test",
                    "test-kid",
                    "file:/unused-private.pem",
                    "file:/unused-public.pem");
            AuthProperties auth = new AuthProperties(
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30),
                    48);
            AccessTokenIssuer issuer = new AccessTokenIssuer(
                    passport,
                    auth,
                    null,
                    Algorithm.HMAC256("ephemeral-test-secret-with-sufficient-length"));
            AccessTokenIssueParam input = AccessTokenIssueParam.builder()
                    .userId("c12c7988-b9fe-4299-a72f-e430d004d37b")
                    .sessionId("3a428175-bef1-4c13-ae53-997b6fbfc508")
                    .instanceId("91ff9854-e8ac-4bf5-91d8-3117dcd8d05c")
                    .roles(List.of("USER"))
                    .userAuthVersion(4L)
                    .build();

            IssuedAccessTokenReceipt result = issuer.issueAccessToken(input);
            DecodedJWT jwt = JWT.decode(result.getToken());

            assertThat(Duration.between(result.getIssuedAt(), result.getExpiresAt()))
                    .isEqualTo(Duration.ofMinutes(15));
            assertThat(Duration.between(jwt.getIssuedAtAsInstant(), jwt.getExpiresAtAsInstant()))
                    .isEqualTo(Duration.ofMinutes(15));
        }
    }
}
