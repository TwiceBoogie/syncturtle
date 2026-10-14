package com.syncturtle.services.user.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.web.context.RequestUserContext;

class UserAuthorizationInterceptorDefaultDenyTest {

    @Test
    void rejectsUnannotatedRouteAndAllowsExplicitAnonymousRoute() throws Exception {
        UserAuthorizationInterceptor interceptor = new UserAuthorizationInterceptor(new RequestUserContext());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod privateRoute = new HandlerMethod(new Routes(), Routes.class.getMethod("privateRoute"));
        HandlerMethod publicRoute = new HandlerMethod(new Routes(), Routes.class.getMethod("publicRoute"));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, privateRoute))
                .isInstanceOf(AuthException.class);
        assertThat(interceptor.preHandle(request, response, publicRoute)).isTrue();
    }

    @Test
    void allowsAuthenticatedCallerOnUnannotatedRoute() throws Exception {
        RequestUserContext context = new RequestUserContext();
        UserAuthorizationInterceptor interceptor = new UserAuthorizationInterceptor(context);
        HandlerMethod privateRoute = new HandlerMethod(new Routes(), Routes.class.getMethod("privateRoute"));

        try {
            context.setAuthenticated(UUID.fromString("f7e6fc27-6a58-4b4b-9215-d6a191c1def1"));

            assertThat(interceptor.preHandle(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), privateRoute)).isTrue();
        } finally {
            context.clear();
        }
    }

    public static final class Routes {
        public void privateRoute() {
        }

        @AllowAnonymous
        public void publicRoute() {
        }
    }
}
