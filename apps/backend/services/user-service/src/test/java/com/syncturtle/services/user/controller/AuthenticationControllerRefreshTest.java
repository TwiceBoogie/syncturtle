package com.syncturtle.services.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.controller.validation.AuthFormValidator;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.RefreshSessionService;

class AuthenticationControllerRefreshTest {

    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final String PRESENTED = SESSION_ID + ".presented";

    private RefreshSessionService refreshSessionService;
    private ServletAuthCookieWriter cookieWriter;
    private AuthenticationController controller;
    private MockHttpServletResponse servletResponse;

    @BeforeEach
    void setUp() {
        refreshSessionService = mock(RefreshSessionService.class);
        cookieWriter = mock(ServletAuthCookieWriter.class);
        controller = new AuthenticationController(
                mock(AuthenticationService.class),
                refreshSessionService,
                cookieWriter,
                mock(AuthFormValidator.class),
                mock(PublicUrlResolver.class));
        servletResponse = new MockHttpServletResponse();
    }

    @Nested
    class Refresh {

        @Test
        void writesRotatedCookiesFromSuccessfulAtomicResponse() {
            // arrange
            Instant now = Instant.parse("2026-08-19T10:00:00Z");
            IssueTokenResponse issued = IssueTokenResponse.builder()
                    .accessToken("access-token")
                    .refreshToken(SESSION_ID + ".successor")
                    .accessIssuedAt(now)
                    .accessExpiresAt(now.plusSeconds(900))
                    .refreshIssuedAt(now)
                    .refreshExpiresAt(now.plusSeconds(3600))
                    .build();
            // conditions
            when(refreshSessionService.refreshSession(PRESENTED, SESSION_ID)).thenReturn(issued);
            // act
            ResponseEntity<Void> result = controller.refresh(servletResponse, SESSION_ID, PRESENTED);
            // assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            // verify
            verify(cookieWriter).setAuthCookies(
                    ArgumentMatchers.eq(servletResponse),
                    ArgumentMatchers.eq("access-token"),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.eq(SESSION_ID + ".successor"),
                    ArgumentMatchers.any());
            verify(cookieWriter, never()).clearCsrfCookie(servletResponse);
        }

        @Test
        void clearsAuthCookiesForRejectedOrReplayedFamily() {
            // arrange
            AuthException rejected = AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED);
            // conditions
            when(refreshSessionService.refreshSession(PRESENTED, SESSION_ID)).thenThrow(rejected);
            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> controller.refresh(servletResponse, SESSION_ID, PRESENTED));
            // assert
            assertThat(failure).isSameAs(rejected);
            // verify
            verify(cookieWriter).clearAuthCookies(servletResponse);
        }

        @Test
        void preservesPreviousCookieWhenRedisOutcomeIsUnavailableForSafeRetry() {
            // arrange
            RemoteServiceException unavailable = RemoteServiceException.unavailable(
                    "session-state",
                    new IllegalStateException("redis unavailable"));
            // conditions
            when(refreshSessionService.refreshSession(PRESENTED, SESSION_ID)).thenThrow(unavailable);
            // act
            RemoteServiceException failure = catchThrowableOfType(
                    RemoteServiceException.class,
                    () -> controller.refresh(servletResponse, SESSION_ID, PRESENTED));
            // assert
            assertThat(failure).isSameAs(unavailable);
            // verify
            verify(cookieWriter, never()).clearAuthCookies(servletResponse);
        }
    }
}
