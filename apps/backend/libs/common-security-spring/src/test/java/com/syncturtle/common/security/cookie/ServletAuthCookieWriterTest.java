package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syncturtle.common.security.property.SecurityCookieProperties;

@DisplayName("ServletAuthCookieWriter")
class ServletAuthCookieWriterTest {

    @Nested
    @DisplayName("setRefreshTokenCookie(HttpServletResponse, String, Duration)")
    class SetRefreshTokenCookieTests {

        @Test
        @DisplayName("deletes legacy variants before setting the managed target")
        void deletesLegacyVariantsBeforeSettingTheManagedTarget() {
            // arrange
            ServletAuthCookieWriter writer = writer(true, true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.setRefreshTokenCookie(response, "new-refresh", Duration.ofDays(7));
            // assert
            Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(headers).hasSize(5);
            assertThat(headers).anyMatch(header -> header.startsWith("__Secure-refresh_token=new-refresh")
                    && header.contains("Path=/auth")
                    && header.contains("Max-Age=604800"));
            assertThat(headers).noneMatch(header -> header.startsWith("__Secure-refresh_token=;")
                    && header.contains("Path=/auth"));
            // verify
        }

        @Test
        @DisplayName("deletes the old root cookie without deleting the local auth target")
        void deletesTheOldRootCookieWithoutDeletingTheLocalAuthTarget() {
            // arrange
            ServletAuthCookieWriter writer = writer(false, false);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.setRefreshTokenCookie(response, "new-refresh", Duration.ofHours(1));
            // assert
            Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(headers).anyMatch(header -> header.startsWith("refresh_token=;")
                    && header.contains("Path=/"));
            assertThat(headers).anyMatch(header -> header.startsWith("refresh_token=new-refresh")
                    && header.contains("Path=/auth"));
            assertThat(headers).noneMatch(header -> header.startsWith("refresh_token=;")
                    && header.contains("Path=/auth"));
            // verify
        }

    }

    @Nested
    @DisplayName("clearAllSecurityCookies(HttpServletResponse)")
    class ClearAllSecurityCookies {

        @Test
        @DisplayName("clears every bounded auth csrf and browser binding identity")
        void clearsEveryBoundedAuthCsrfAndBrowserBindingIdentity() {
            // arrange
            ServletAuthCookieWriter writer = writer(true, true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.clearAllSecurityCookies(response);
            // assert
            Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(headers).hasSize(11);
            assertClearingCookie(headers, "access_token", "/", "Lax");
            assertClearingCookie(headers, "__Host-access_token", "/", "Lax");
            assertClearingCookie(headers, "refresh_token", "/", "Lax");
            assertClearingCookie(headers, "refresh_token", "/auth", "Lax");
            assertClearingCookie(headers, "__Host-refresh_token", "/", "Lax");
            assertClearingCookie(headers, "__Secure-refresh_token", "/", "Lax");
            assertClearingCookie(headers, "__Secure-refresh_token", "/auth", "Lax");
            assertClearingCookie(headers, "csrf_token", "/", "Lax");
            assertClearingCookie(headers, "__Host-csrf_token", "/", "Lax");
            assertClearingCookie(headers, "__Host-st_admin_handoff_binding", "/", "Strict");
            assertClearingCookie(headers, "__Host-st_oauth_browser_binding", "/", "Lax");
            // verify
        }

    }

    private static void assertClearingCookie(Collection<String> headers, String name, String path, String sameSite) {
        assertThat(headers)
                .filteredOn(header -> header.startsWith(name + "=")
                        && Arrays.asList(header.split("; ")).contains("Path=" + path))
                .singleElement()
                .satisfies(header -> {
                    List<String> attributes = Arrays.asList(header.split("; "));
                    assertThat(attributes.getFirst()).isEqualTo(name + "=");
                    assertThat(attributes).contains("Path=" + path, "Max-Age=0", "Secure", "HttpOnly",
                            "SameSite=" + sameSite);
                    assertThat(attributes).noneMatch(attribute -> attribute.startsWith("Domain="));
                });
    }

    private static ServletAuthCookieWriter writer(boolean secure, boolean managedPrefixEnabled) {
        SecurityCookieProperties properties = new SecurityCookieProperties(
                secure,
                managedPrefixEnabled,
                "Lax",
                "Lax",
                "Lax");
        return new ServletAuthCookieWriter(new SecurityCookieFactory(properties));
    }

}
