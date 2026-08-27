package com.syncturtle.services.user.controller;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_SESSION_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.UserSessionInventoryResponse;
import com.syncturtle.services.user.dto.response.UserSessionRevocationResponse;
import com.syncturtle.services.user.service.UserSessionService;

@ExtendWith(MockitoExtension.class)
class UserSessionControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CURRENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private UserSessionService service;
    @Mock
    private ServletAuthCookieWriter cookieWriter;

    private UserSessionController controller;

    @BeforeEach
    void setup() {
        controller = new UserSessionController(service, cookieWriter);
    }

    @Nested
    class ListSessionsTests {

        @Test
        void delegatesOneServiceOperationAndReturnsNoStore() {
            // arrange
            UserSessionInventoryResponse serviceResponse = new UserSessionInventoryResponse(List.of());
            // conditions
            when(service.listSessions(USER_ID, CURRENT_ID)).thenReturn(serviceResponse);
            // act
            ResponseEntity<UserSessionInventoryResponse> result = controller.listSessions(USER_ID, CURRENT_ID);
            // assert
            assertThat(result.getBody()).isSameAs(serviceResponse);
            assertThat(result.getHeaders().getCacheControl()).isEqualTo("no-store");
            // verify
            verify(service).listSessions(USER_ID, CURRENT_ID);
            verifyNoCookieClear();
        }

    }

    @Nested
    class RevokeSessionTests {

        @Test
        void clearsCookiesOnlyWhenServiceSaysCurrentWasRevoked() {
            // arrange
            MockHttpServletResponse currentResponse = new MockHttpServletResponse();
            MockHttpServletResponse otherResponse = new MockHttpServletResponse();
            // conditions
            when(service.revokeSession(USER_ID, CURRENT_ID, CURRENT_ID))
                    .thenReturn(new UserSessionRevocationResponse(true));
            when(service.revokeSession(USER_ID, CURRENT_ID, OTHER_ID))
                    .thenReturn(new UserSessionRevocationResponse(false));
            // act
            ResponseEntity<UserSessionRevocationResponse> current = controller.revokeSession(
                    USER_ID,
                    CURRENT_ID,
                    CURRENT_ID,
                    currentResponse);
            ResponseEntity<UserSessionRevocationResponse> other = controller.revokeSession(
                    USER_ID,
                    CURRENT_ID,
                    OTHER_ID,
                    otherResponse);
            // assert
            assertThat(current.getBody().isCurrentSessionRevoked()).isTrue();
            assertThat(other.getBody().isCurrentSessionRevoked()).isFalse();
            // verify
            verify(cookieWriter).clearAllSecurityCookies(currentResponse);
            verify(cookieWriter, never()).clearAllSecurityCookies(otherResponse);
        }

        @Test
        void failureDoesNotClearCookiesOrReturnSuccess() {
            // arrange
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            // conditions
            when(service.revokeSession(USER_ID, CURRENT_ID, CURRENT_ID)).thenThrow(
                    RemoteServiceException.unavailable("session-state", new IllegalStateException("redis")));
            // act
            Throwable failure = catchThrowable(() -> controller.revokeSession(
                    USER_ID,
                    CURRENT_ID,
                    CURRENT_ID,
                    servletResponse));
            // assert
            assertThat(failure).isInstanceOf(RemoteServiceException.class);
            // verify
            verify(cookieWriter, never()).clearAllSecurityCookies(any());
        }

    }

    @Nested
    class RevokeOtherSessionsTests {

        @Test
        void literalOthersRouteDoesNotReachUuidTargetOperation() throws Exception {
            // arrange
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new CurrentUserResolver())
                    .build();
            when(service.revokeOtherSessions(USER_ID, CURRENT_ID))
                    .thenReturn(new UserSessionRevocationResponse(false));
            // act + assert
            mockMvc.perform(delete("/api/users/me/sessions/others")
                    .header(HDR_AUTH_SESSION_ID, CURRENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"));
            // verify
            verify(service).revokeOtherSessions(USER_ID, CURRENT_ID);
            verify(service, never()).revokeSession(any(), any(), any());
            verifyNoCookieClear();
        }

    }

    @Nested
    class RevokeAllSessionsTests {

        @Test
        void clearsCookiesAfterSuccessfulCurrentRevocation() {
            // arrange
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            // conditions
            when(service.revokeAllSessions(USER_ID, CURRENT_ID))
                    .thenReturn(new UserSessionRevocationResponse(true));
            // act
            ResponseEntity<UserSessionRevocationResponse> result = controller.revokeAllSessions(
                    USER_ID,
                    CURRENT_ID,
                    servletResponse);
            // assert
            assertThat(result.getBody().isCurrentSessionRevoked()).isTrue();
            assertThat(result.getHeaders().getCacheControl()).isEqualTo("no-store");
            // verify
            verify(cookieWriter).clearAllSecurityCookies(servletResponse);
        }

    }

    private void verifyNoCookieClear() {
        verify(cookieWriter, never()).clearAllSecurityCookies(any());
    }

    private static final class CurrentUserResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return USER_ID;
        }

    }

}
