package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syncturtle.common.security.property.SecurityCookieProperties;

class AdminSessionHandoffCookieTest {

    @Nested
    class AdminSessionHandoffCookieTests {

        @Test
        void createsAnHttpOnlyStrictPathScopedHostOnlyCookie() {
            // arrange
            SecurityCookieFactory factory = new SecurityCookieFactory(properties(false, false));
            // conditions
            // act
            ResponseCookie cookie = factory.adminSessionHandoffCookie("receipt.secret", Duration.ofSeconds(30));
            // assert
            assertThat(cookie.getName()).isEqualTo("admin_session_handoff");
            assertThat(cookie.getPath()).isEqualTo("/auth/admin/session");
            assertThat(cookie.getDomain()).isNull();
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isFalse();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(30));
            // verify
        }

        @Test
        void usesSecurePrefixInsteadOfHostPrefixForTheNarrowPath() {
            // arrange
            SecurityCookieFactory factory = new SecurityCookieFactory(properties(true, true));
            // conditions
            // act
            ResponseCookie cookie = factory.adminSessionHandoffCookie("receipt.secret", Duration.ofSeconds(30));
            // assert
            assertThat(cookie.getName()).isEqualTo("__Secure-admin_session_handoff");
            assertThat(cookie.isSecure()).isTrue();
            // verify
        }

        @Test
        void clearsWithTheExactCreationAttributes() {
            // arrange
            SecurityCookieFactory factory = new SecurityCookieFactory(properties(true, true));
            // conditions
            // act
            ResponseCookie cookie = factory.clearAdminSessionHandoffCookie();
            // assert
            assertThat(cookie.getName()).isEqualTo("__Secure-admin_session_handoff");
            assertThat(cookie.getPath()).isEqualTo("/auth/admin/session");
            assertThat(cookie.getDomain()).isNull();
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
            // verify
        }

    }

    @Nested
    class ServletWriter {

        @Test
        void clearingHandoffDoesNotClearAccessOrRefreshCookies() {
            // arrange
            SecurityCookieFactory factory = new SecurityCookieFactory(properties(false, false));
            ServletAuthCookieWriter writer = new ServletAuthCookieWriter(factory);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.clearAdminSessionHandoffCookie(response);
            // assert
            assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                    .singleElement()
                    .asString()
                    .startsWith("admin_session_handoff=")
                    .doesNotContain("access_token", "refresh_token");
            // verify
        }

        @Test
        void settingFinalAuthCookiesThenClearingHandoffPreservesFinalTargets() {
            // arrange
            SecurityCookieFactory factory = new SecurityCookieFactory(properties(true, true));
            ServletAuthCookieWriter writer = new ServletAuthCookieWriter(factory);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.setAuthCookies(response, "access", Duration.ofMinutes(15), "refresh", Duration.ofDays(7));
            writer.clearAdminSessionHandoffCookie(response);
            // assert
            assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                    .anyMatch(header -> header.startsWith("__Host-access_token=access"))
                    .anyMatch(header -> header.startsWith("__Secure-refresh_token=refresh")
                            && header.contains("Path=/auth"))
                    .anyMatch(header -> header.startsWith("__Secure-admin_session_handoff=")
                            && header.contains("Max-Age=0"));
            // verify
        }

    }

    private static SecurityCookieProperties properties(boolean secure, boolean managedPrefixEnabled) {
        return new SecurityCookieProperties(secure, managedPrefixEnabled, "Lax", "Lax", "Lax", Duration.ofHours(1));
    }

}
