package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

@DisplayName("PassportClaims")
class PassportClaimsTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String USER_ID = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    private static final String SESSION_ID = "3a428175-bef1-4c13-ae53-997b6fbfc508";
    private static final String INSTANCE_ID = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";

    @Nested
    @DisplayName("From(Jwt, Clock)")
    class FromTests {

        @Test
        @DisplayName("rejects missing subject")
        void rejectsMissingSubject() {
            // arrange
            Jwt jwt = validJwt().subject(null).build();
            // conditions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
            // verify
        }

        @Test
        @DisplayName("rejects malformed subject")
        void rejectsMalformedSubject() {
            // arrange
            Jwt jwt = validJwt().subject("not-a-uuid").build();
            // conditions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
            // verify
        }

        @Test
        @DisplayName("rejects missing sessionId")
        void rejectsMissingSessionId() {
            // arrange
            Jwt jwt = validJwt().claim("sid", null).build();
            // condtions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
            // verify
        }

        @Test
        @DisplayName("rejects malformed sessionId")
        void rejectsMalformedSessionId() {
            // arrange
            Jwt jwt = validJwt().claim("sid", "not-a-uuid").build();
            // conditions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
        }

        @Test
        @DisplayName("rejects future issued at beyond allowed clock skew")
        void rejectsFutureIssuedAtBeyoundAllowedClockSkew() {
            // arrange
            Jwt jwt = validJwt().issuedAt(NOW.plusSeconds(61)).build();
            // conditions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
            // verify
        }

        @Test
        @DisplayName("rejects duplicate roles")
        void rejectsDuplicateRoles() {
            // arrange
            Jwt jwt = validJwt().claim("roles", List.of("USER", "USER")).build();
            // conditions
            // act
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> PassportClaims.from(jwt, CLOCK));
            // assert
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
        }

    }

    private static Jwt.Builder validJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://api.syncturtle.test/auth")
                .audience(List.of("syncturtle-test"))
                .subject(USER_ID)
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(900))
                .claim("sid", SESSION_ID)
                .claim("instance_id", INSTANCE_ID)
                .claim("roles", List.of("USER"))
                .claim("auth_ver", 4L)
                .claim("token_use", "access");
    }

}
