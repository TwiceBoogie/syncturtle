package com.syncturtle.services.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.controller.validation.AuthFormValidator;
import com.syncturtle.services.user.dto.response.SignOutResponse;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.RefreshSessionService;

import jakarta.servlet.http.HttpServletResponse;

class AuthenticationControllerSignOutTest {

    private static final String REFRESH_TOKEN = "3a428175-bef1-4c13-ae53-997b6fbfc508.refresh-secret";
    private static final String SESSION_ID = "3a428175-bef1-4c13-ae53-997b6fbfc508";

    @Nested
    class SignOut {

        @Test
        void bindsPresentedRefreshTokenFromConfiguredCookie() throws Exception {
            // arrange
            Method method = AuthenticationController.class.getDeclaredMethod(
                    "signOut",
                    String.class,
                    String.class,
                    String.class,
                    HttpServletResponse.class);
            // act
            CookieValue cookieValue = method.getParameters()[2].getAnnotation(CookieValue.class);
            // assert
            assertThat(cookieValue).isNotNull();
            assertThat(cookieValue.name()).isEqualTo("#{@securityCookieFactory.refreshCookieName()}");
            assertThat(cookieValue.required()).isFalse();
            // verify
        }

        @Test
        void clearsEverySecurityCookieAndDelegatesOnePublicUseCase() {
            // arrange
            AuthenticationService authenticationService = mock(AuthenticationService.class);
            RefreshSessionService refreshSessionService = mock(RefreshSessionService.class);
            ServletAuthCookieWriter cookieWriter = mock(ServletAuthCookieWriter.class);
            AuthFormValidator authFormValidator = mock(AuthFormValidator.class);
            PublicUrlResolver hostResolver = mock(PublicUrlResolver.class);
            AuthenticationController controller = new AuthenticationController(
                    authenticationService,
                    refreshSessionService,
                    cookieWriter,
                    authFormValidator,
                    hostResolver);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            // conditions
            when(authenticationService.signOut("WEB", REFRESH_TOKEN, SESSION_ID))
                    .thenReturn(SignOutResponse.redirect("https://syncturtle.test/sign-in"));
            // act
            ResponseEntity<Void> result = controller.signOut("WEB", SESSION_ID, REFRESH_TOKEN, servletResponse);
            // assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
            assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("https://syncturtle.test/sign-in"));
            // verify
            InOrder order = inOrder(cookieWriter, authenticationService);
            order.verify(authenticationService).signOut("WEB", REFRESH_TOKEN, SESSION_ID);
            order.verify(cookieWriter).clearAllSecurityCookies(servletResponse);
        }
    }
}
