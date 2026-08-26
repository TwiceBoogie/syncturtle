package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.syncturtle.common.security.property.SecurityCookieProperties;

@DisplayName("SecurityCookieFactory")
class SecurityCookieFactoryTest {

    @Nested
    @DisplayName("accessTokenCookie(String, Duration)")
    class AccessTokenCookieTests {

        @Test
        @DisplayName("creates host only root cookie in local mode")
        void createsHostOnlyRootCookieInLocalMode() {
            // arrange
            SecurityCookieFactory factory = factory(false, false);
            // conditions
            // act
            ResponseCookie cookie = factory.accessTokenCookie("access", Duration.ofMinutes(15));
            // assert
            assertCookie(cookie, "access_token", "/", false, "Lax", Duration.ofMinutes(15));
            // verify
        }

        @Test
        @DisplayName("uses host prefix in managed secure mode")
        void usesHostPrefixInManagedSecureMode() {
            // arrange
            SecurityCookieFactory factory = factory(true, true);
            // conditions
            // act
            ResponseCookie cookie = factory.accessTokenCookie("access", Duration.ofMinutes(15));
            // assert
            assertCookie(cookie, "__Host-access_token", "/", true, "Lax", Duration.ofMinutes(15));
            // verify
        }

    }

    @Nested
    @DisplayName("refreshTokenCookie(String, Duration)")
    class RefreshTokenCookieTests {

        @Test
        @DisplayName("creates host only auth scoped cookie in local mode")
        void createsHostOnlyAuthScopedCookieInLocalMode() {
            // arrange
            SecurityCookieFactory factory = factory(false, false);
            // conditions
            // act
            ResponseCookie cookie = factory.refreshTokenCookie("refresh", Duration.ofDays(7));
            // assert
            assertCookie(cookie, "refresh_token", "/auth", false, "Lax", Duration.ofDays(7));
            // verify
        }

        @Test
        @DisplayName("uses secure prefix and never host prefix at the narrow path")
        void usesSecurePrefixAndNeverHostPrefixAtTheNarrowPath() {
            // arrange
            SecurityCookieFactory factory = factory(true, true);
            // conditions
            // act
            ResponseCookie cookie = factory.refreshTokenCookie("refresh", Duration.ofDays(7));
            // assert
            assertCookie(cookie, "__Secure-refresh_token", "/auth", true, "Lax", Duration.ofDays(7));
            assertThat(cookie.getName()).doesNotStartWith("__Host-");
            // verify
        }

        @Test
        @DisplayName("exposes only the bounded temporary migration names")
        void exposesOnlyTheBoundedTemporaryMigrationNames() {
            // arrange
            SecurityCookieFactory factory = factory(false, false);
            // conditions
            // act
            List<String> names = factory.acceptedRefreshCookieNames();
            // assert
            assertThat(names).containsExactly(
                    "refresh_token",
                    "__Host-refresh_token",
                    "__Secure-refresh_token");
            // verify
        }

        @Test
        @DisplayName("clears every legacy name and path identity")
        void clearsEveryLegacyNameAndPathIdentity() {
            // arrange
            SecurityCookieFactory factory = factory(true, true);
            // conditions
            // act
            List<ResponseCookie> cookies = factory.clearRefreshTokenCookies();
            // assert
            assertThat(cookies)
                    .extracting(cookie -> cookie.getName() + " " + cookie.getPath())
                    .containsExactly(
                            "refresh_token /",
                            "refresh_token /auth",
                            "__Host-refresh_token /",
                            "__Secure-refresh_token /",
                            "__Secure-refresh_token /auth");
            assertThat(cookies).allSatisfy(cookie -> {
                assertThat(cookie.getDomain()).isNull();
                assertThat(cookie.isHttpOnly()).isTrue();
                assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
            });
            // verify
        }

        @Test
        @DisplayName("excludes the active target from issuance cleanup")
        void excludesTheActiveTargetFromIssuanceCleanup() {
            // arrange
            SecurityCookieFactory factory = factory(true, true);
            // conditions
            // act
            List<ResponseCookie> cookies = factory.legacyRefreshTokenCookies();
            // assert
            assertThat(cookies)
                    .noneMatch(cookie -> "__Secure-refresh_token".equals(cookie.getName())
                            && "/auth".equals(cookie.getPath()));
            // verify
        }

    }

    @Nested
    @DisplayName("csrfCookie(String)")
    class CsrfCookie {

        @Test
        @DisplayName("preserves root path and managed host prefix")
        void preservesRootPathAndManagedHostPrefix() {
            // arrange
            SecurityCookieFactory factory = factory(true, true);
            // conditions
            // act
            ResponseCookie cookie = factory.csrfCookie("signed.csrf");
            // assert
            assertCookie(cookie, "__Host-csrf_token", "/", true, "Lax", Duration.ofMinutes(30));
            // verify
        }

    }

    private static SecurityCookieFactory factory(boolean secure, boolean managedPrefixEnabled) {
        SecurityCookieProperties properties = new SecurityCookieProperties(
                secure,
                managedPrefixEnabled,
                "Lax",
                "Lax",
                "Lax",
                Duration.ofMinutes(30));
        return new SecurityCookieFactory(properties);
    }

    private static void assertCookie(
            ResponseCookie cookie,
            String name,
            String path,
            boolean secure,
            String sameSite,
            Duration maxAge) {
        assertThat(cookie.getName()).isEqualTo(name);
        assertThat(cookie.getPath()).isEqualTo(path);
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isEqualTo(secure);
        assertThat(cookie.getSameSite()).isEqualTo(sameSite);
        assertThat(cookie.getMaxAge()).isEqualTo(maxAge);
    }

}
