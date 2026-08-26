package com.syncturtle.services.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.service.AdminSessionCompletionService;
import com.syncturtle.services.user.service.param.AdminSessionCompletionParam;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("AdminSessionCompletionController")
class AdminSessionCompletionControllerTest {

    private static final String HANDOFF_COOKIE = "admin_session_handoff";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String COMPLETION_CODE = "33333333-3333-3333-3333-333333333333.opaque";

    @Mock
    AdminSessionCompletionService service;
    @Mock
    ServletAuthCookieWriter cookieWriter;
    @Mock
    RequestClientContext requestClientContext;
    @Mock
    PublicUrlResolver hostResolver;

    private AdminSessionCompletionController controller;

    @BeforeEach
    void setup() {
        controller = new AdminSessionCompletionController(service, cookieWriter, requestClientContext, hostResolver);
    }

    @Nested
    @DisplayName("complete(request, response)")
    class CompleteTests {

        @Test
        @DisplayName("writes final credentials, clears only the handoff, and redirects")
        void writesFinalCredentialsClearsHandoffAndRedirects() {
            // arrange
            MockHttpServletRequest request = validRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            Instant now = Instant.parse("2026-08-21T15:00:00Z");
            IssueTokenResponse issued = IssueTokenResponse.builder()
                    .accessToken("access")
                    .refreshToken("refresh")
                    .accessIssuedAt(now)
                    .accessExpiresAt(now.plusSeconds(900))
                    .refreshIssuedAt(now)
                    .refreshExpiresAt(now.plusSeconds(3600))
                    .location("https://admin.example.test/general")
                    .build();
            // conditions
            stubCookieNames();
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(service.complete(any(AdminSessionCompletionParam.class))).thenReturn(issued);
            // act
            ResponseEntity<Void> result = controller.complete(request, response);
            // assert
            assertThat(result.getStatusCode().value()).isEqualTo(303);
            assertThat(result.getHeaders().getLocation())
                    .hasToString("https://admin.example.test/general");
            // verify
            verify(cookieWriter).setAuthCookies(
                    response,
                    "access",
                    Duration.ofSeconds(900),
                    "refresh",
                    Duration.ofSeconds(3600));
            verify(cookieWriter).clearAdminSessionHandoffCookie(response);
            verify(cookieWriter, never()).clearAuthCookies(response);
        }

        @Test
        @DisplayName("clears invalid or replayed handoff and redirects to admin sign-in")
        void clearsInvalidHandoffAndRedirects() {
            // arrange
            MockHttpServletRequest request = validRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AdminSessionHandoffException invalid = new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.REPLAYED,
                    "replayed");
            // conditions
            stubCookieNames();
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(service.complete(any(AdminSessionCompletionParam.class))).thenThrow(invalid);
            when(hostResolver.admin("")).thenReturn("https://admin.example.test");
            // act
            ResponseEntity<Void> result = controller.complete(request, response);
            // assert
            assertThat(result.getHeaders().getLocation()).hasToString("https://admin.example.test");
            verify(cookieWriter).clearAdminSessionHandoffCookie(response);
            verify(cookieWriter, never()).clearAuthCookies(response);
        }

        @Test
        @DisplayName("rejects missing or duplicate handoff cookies without calling the service")
        void rejectsMissingOrDuplicateHandoffCookies() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(
                    new Cookie(HANDOFF_COOKIE, COMPLETION_CODE),
                    new Cookie(HANDOFF_COOKIE, COMPLETION_CODE),
                    new Cookie(CSRF_COOKIE, "signed.csrf"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            // conditions
            stubCookieNames();
            when(hostResolver.admin("")).thenReturn("https://admin.example.test");
            // act
            ResponseEntity<Void> result = controller.complete(request, response);
            // assert
            assertThat(result.getStatusCode().value()).isEqualTo(303);
            // verify
            verifyNoInteractions(service, requestClientContext);
            verify(cookieWriter).clearAdminSessionHandoffCookie(response);
        }

        @Test
        @DisplayName("preserves the handoff cookie on infrastructure failure for bounded retry")
        void preservesHandoffCookieOnInfrastructureFailure() {
            // arrange
            MockHttpServletRequest request = validRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AdminSessionHandoffException unavailable = new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.REDIS_UNAVAILABLE,
                    "unavailable");
            // conditions
            stubCookieNames();
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(service.complete(any(AdminSessionCompletionParam.class))).thenThrow(unavailable);
            // act + assert
            assertThatThrownBy(() -> controller.complete(request, response)).isSameAs(unavailable);
            // verify
            verify(cookieWriter, never()).clearAdminSessionHandoffCookie(response);
            verify(cookieWriter, never()).clearAuthCookies(response);
        }

        private MockHttpServletRequest validRequest() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(
                    new Cookie(HANDOFF_COOKIE, COMPLETION_CODE),
                    new Cookie(CSRF_COOKIE, "signed.csrf"));
            return request;
        }

        private void stubCookieNames() {
            when(cookieWriter.adminSessionHandoffCookieName()).thenReturn(HANDOFF_COOKIE);
            when(cookieWriter.csrfCookieName()).thenReturn(CSRF_COOKIE);
        }

    }

}
