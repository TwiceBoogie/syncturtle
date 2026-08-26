package com.syncturtle.services.instance.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.method.HandlerMethod;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.type.InstanceAdminRole;
import com.syncturtle.services.instance.type.InstanceAdminRoleCodes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceAuthorizationInterceptor")
class InstanceAuthorizationInterceptorTest {

    private static final UUID USER_ID = UUID.fromString("f7e6fc27-6a58-4b4b-9215-d6a191c1def1");

    @Mock
    InstanceAuthorizationService instanceAuthorizationService;

    private RequestUserContext requestUserContext;
    private InstanceAuthorizationInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setup() {
        requestUserContext = new RequestUserContext();
        interceptor = new InstanceAuthorizationInterceptor(requestUserContext, instanceAuthorizationService, false);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @AfterEach
    void cleanup() {
        requestUserContext.clear();
    }

    @Nested
    @DisplayName("preHandle(HttpServletRequest, HttpServletResponse, Object)")
    class PreHandleTests {

        @Test
        @DisplayName("rejects an anonymous caller before checking an annotated role")
        void rejectsAnonymousCallerBeforeCheckingAnnotatedRole() throws Exception {
            // arrange
            HandlerMethod handler = handler("adminOnly");
            // act
            Throwable thrown = catchThrowable(() -> interceptor.preHandle(request, response, handler));
            // assert
            assertThat(thrown).isInstanceOf(AuthException.class);
            AuthException exception = (AuthException) thrown;
            assertThat(exception.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        @Test
        @DisplayName("rejects an authenticated caller without the annotated role")
        void rejectsAuthenticatedCallerWithoutAnnotatedRole() throws Exception {
            // arrange
            requestUserContext.setAuthenticated(USER_ID);
            HandlerMethod handler = handler("adminOnly");
            // conditions
            when(instanceAuthorizationService.hasCurrentInstanceRoleAtLeast(USER_ID, InstanceAdminRole.ADMIN))
                    .thenReturn(false);
            // act
            Throwable thrown = catchThrowable(() -> interceptor.preHandle(request, response, handler));
            // assert
            assertThat(thrown).isInstanceOf(AuthException.class);
            AuthException exception = (AuthException) thrown;
            assertThat(exception.getAuthErrorCode()).isEqualTo(AuthErrorCode.INSUFFICIENT_INSTANCE_ROLE);
            // verify
            verify(instanceAuthorizationService).hasCurrentInstanceRoleAtLeast(USER_ID, InstanceAdminRole.ADMIN);
        }

        @Test
        @DisplayName("allows an authenticated caller with the annotated role")
        void allowsAuthenticatedCallerWithAnnotatedRole() throws Exception {
            // arrange
            requestUserContext.setAuthenticated(USER_ID);
            HandlerMethod handler = handler("adminOnly");
            // conditions
            when(instanceAuthorizationService.hasCurrentInstanceRoleAtLeast(USER_ID, InstanceAdminRole.ADMIN))
                    .thenReturn(true);
            // act
            boolean allowed = interceptor.preHandle(request, response, handler);
            // assert
            assertThat(allowed).isTrue();
            // verify
            verify(instanceAuthorizationService).hasCurrentInstanceRoleAtLeast(USER_ID, InstanceAdminRole.ADMIN);
        }

        @Test
        @DisplayName("abstains for an unannotated handler when authentication is not required by default")
        void abstainsForUnannotatedHandler() throws Exception {
            // arrange
            HandlerMethod handler = handler("unannotated");
            // act
            boolean allowed = interceptor.preHandle(request, response, handler);
            // assert
            assertThat(allowed).isTrue();
            // verify
            verifyNoInteractions(instanceAuthorizationService);
        }

        @Test
        @DisplayName("rejects an unknown role code before calling the authorization service")
        void rejectsUnknownRoleCode() throws Exception {
            // arrange
            requestUserContext.setAuthenticated(USER_ID);
            HandlerMethod handler = handler("unknownRole");
            // act
            Throwable thrown = catchThrowable(() -> interceptor.preHandle(request, response, handler));
            // assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not map to a known InstanceAdminRole");
            // verify
            verifyNoInteractions(instanceAuthorizationService);
        }

    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod(methodName));
    }

    private static final class TestController {

        @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
        public void adminOnly() {
        }

        @RequireInstancePermission(minRole = Integer.MAX_VALUE)
        public void unknownRole() {
        }

        @SuppressWarnings("unused")
        public void unannotated() {
        }
    }

}
