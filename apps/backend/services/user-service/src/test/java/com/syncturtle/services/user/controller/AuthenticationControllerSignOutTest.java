package com.syncturtle.services.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;

import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.controller.validation.AuthFormValidator;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.RefreshSessionService;

import jakarta.servlet.http.HttpServletResponse;

class AuthenticationControllerSignOutTest {

    private static final String PRESENTED = "33333333-3333-3333-3333-333333333333.presented";

    @Test
    void bindsOnlyTheRefreshCookieAndRequiresNoAccessJwt() throws Exception {
        Method method = AuthenticationController.class.getDeclaredMethod("signOut", String.class,
                HttpServletResponse.class);
        CookieValue cookie = method.getParameters()[0].getAnnotation(CookieValue.class);
        assertThat(cookie.name()).isEqualTo("#{@securityCookieFactory.refreshCookieName()}");
        assertThat(cookie.required()).isFalse();
    }

    @Test
    void successfulRevocationClearsCookiesAndReturnsNoStore() {
        AuthenticationService service = mock(AuthenticationService.class);
        ServletAuthCookieWriter cookies = mock(ServletAuthCookieWriter.class);
        AuthenticationController controller = controller(service, cookies);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.signOut(PRESENTED, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getHeaders().getCacheControl()).contains("no-store");
        InOrder order = inOrder(service, cookies);
        order.verify(service).signOut(PRESENTED);
        order.verify(cookies).clearAllSecurityCookies(response);
        order.verify(cookies).clearAdminSessionHandoffCookie(response);
    }

    @Test
    void staleLogoutCannotClearSuccessorCookies() {
        AuthenticationService service = mock(AuthenticationService.class);
        ServletAuthCookieWriter cookies = mock(ServletAuthCookieWriter.class);
        AuthenticationController controller = controller(service, cookies);
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)).when(service).signOut(PRESENTED);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.signOut(PRESENTED, response))
                .isInstanceOf(AuthException.class);
        verify(cookies, never()).clearAllSecurityCookies(response);
        verify(cookies, never()).clearAdminSessionHandoffCookie(response);
    }

    private static AuthenticationController controller(AuthenticationService service, ServletAuthCookieWriter cookies) {
        return new AuthenticationController(service, mock(RefreshSessionService.class), cookies,
                mock(AuthFormValidator.class), mock(PublicUrlResolver.class));
    }
}
