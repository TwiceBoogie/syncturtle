package com.syncturtle.services.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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

    private static final String PRESENTED = "33333333-3333-3333-3333-333333333333.presented";
    private RefreshSessionService refreshService;
    private ServletAuthCookieWriter cookies;
    private AuthenticationController controller;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        refreshService = mock(RefreshSessionService.class);
        cookies = mock(ServletAuthCookieWriter.class);
        controller = new AuthenticationController(mock(AuthenticationService.class), refreshService, cookies,
                mock(AuthFormValidator.class), mock(PublicUrlResolver.class));
        response = new MockHttpServletResponse();
    }

    @Test
    void missingRefreshCookieNeverClearsNewerCookies() {
        var result = controller.refresh(response, null);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getHeaders().getCacheControl()).contains("no-store");
        verify(cookies, never()).clearAuthCookies(response);
        verify(cookies, never()).clearAllSecurityCookies(response);
    }

    @Test
    void expiredAccessCookieIsNotRequiredForRefresh() {
        Instant now = Instant.parse("2026-10-17T12:00:00Z");
        IssueTokenResponse issued = IssueTokenResponse.builder()
                .accessToken("new-access").refreshToken("new-refresh")
                .accessIssuedAt(now).accessExpiresAt(now.plusSeconds(3600))
                .refreshIssuedAt(now).refreshExpiresAt(now.plusSeconds(3600)).build();
        when(refreshService.refreshSession(PRESENTED)).thenReturn(issued);
        var result = controller.refresh(response, PRESENTED);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getHeaders().getCacheControl()).contains("no-store");
        verify(cookies).setAuthCookies(eq(response), eq("new-access"), any(), eq("new-refresh"), any());
        verify(cookies, never()).clearAuthCookies(response);
    }

    @Test
    void staleRefreshFailureCannotClearAConcurrentSuccessorCookie() {
        AuthException stale = AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED);
        when(refreshService.refreshSession(PRESENTED)).thenThrow(stale);

        assertThatThrownBy(() -> controller.refresh(response, PRESENTED)).isSameAs(stale);
        verify(cookies, never()).clearAuthCookies(response);
        verify(cookies, never()).clearAllSecurityCookies(response);
    }

    @Test
    void redisOutageDoesNotClearThePresentedCredential() {
        RemoteServiceException unavailable = RemoteServiceException.unavailable(
                "session-state", new IllegalStateException("redis unavailable"));
        when(refreshService.refreshSession(PRESENTED)).thenThrow(unavailable);

        assertThatThrownBy(() -> controller.refresh(response, PRESENTED)).isSameAs(unavailable);
        verify(cookies, never()).clearAuthCookies(response);
        verify(cookies, never()).clearAllSecurityCookies(response);
    }
}
