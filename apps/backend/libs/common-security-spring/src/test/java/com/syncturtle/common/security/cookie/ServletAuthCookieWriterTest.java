package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collection;

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
        @DisplayName("clears every bounded access refresh and csrf identity")
        void clearsEveryBoundedAccessRefreshAndCsrfIdentity() {
            // arrange
            ServletAuthCookieWriter writer = writer(true, true);
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            // act
            writer.clearAllSecurityCookies(response);
            // assert
            Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(headers).hasSize(9);
            assertThat(headers).allMatch(header -> header.contains("Max-Age=0"));
            assertThat(headers).anyMatch(header -> header.startsWith("__Host-access_token="));
            assertThat(headers).anyMatch(header -> header.startsWith("__Host-refresh_token="));
            assertThat(headers).anyMatch(header -> header.startsWith("__Secure-refresh_token="));
            assertThat(headers).anyMatch(header -> header.startsWith("__Host-csrf_token="));
            // verify
        }

    }

    private static ServletAuthCookieWriter writer(boolean secure, boolean managedPrefixEnabled) {
        SecurityCookieProperties properties = new SecurityCookieProperties(
                secure,
                managedPrefixEnabled,
                "Lax",
                "Lax",
                "Lax",
                Duration.ofMinutes(30));
        return new ServletAuthCookieWriter(new SecurityCookieFactory(properties));
    }

}
