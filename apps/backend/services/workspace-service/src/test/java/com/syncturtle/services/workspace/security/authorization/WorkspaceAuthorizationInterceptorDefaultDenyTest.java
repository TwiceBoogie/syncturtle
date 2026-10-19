package com.syncturtle.services.workspace.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRoleCodes;
import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;

class WorkspaceAuthorizationInterceptorDefaultDenyTest {

    private static final UUID USER_ID = UUID.fromString("f7e6fc27-6a58-4b4b-9215-d6a191c1def1");

    @Test
    void rejectsUnannotatedWorkspaceRouteWithoutAuthentication() throws Exception {
        WorkspaceAuthorizationInterceptor interceptor = new WorkspaceAuthorizationInterceptor(
                new RequestUserContext(), mock(WorkspaceAuthorizationService.class));
        HandlerMethod handler = new HandlerMethod(new Routes(), Routes.class.getMethod("privateRoute"));

        assertThatThrownBy(() -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handler))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void allowsAuthenticatedUnannotatedRouteAndExplicitAnonymousRoute() throws Exception {
        RequestUserContext context = new RequestUserContext();
        WorkspaceAuthorizationInterceptor interceptor = new WorkspaceAuthorizationInterceptor(
                context, mock(WorkspaceAuthorizationService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            context.setAuthenticated(USER_ID);
            assertThat(interceptor.preHandle(request, response,
                    new HandlerMethod(new Routes(), Routes.class.getMethod("privateRoute")))).isTrue();

            context.setAnonymous();
            assertThat(interceptor.preHandle(request, response,
                    new HandlerMethod(new Routes(), Routes.class.getMethod("publicRoute")))).isTrue();
        } finally {
            context.clear();
        }
    }

    @Test
    void deniesAuthenticatedCallerWithoutRequiredWorkspaceRole() throws Exception {
        RequestUserContext context = new RequestUserContext();
        WorkspaceAuthorizationService service = mock(WorkspaceAuthorizationService.class);
        WorkspaceAuthorizationInterceptor interceptor = new WorkspaceAuthorizationInterceptor(context, service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("workspaceSlug", "test-workspace");
        HandlerMethod handler = new HandlerMethod(new Routes(), Routes.class.getMethod("adminRoute"));

        try {
            context.setAuthenticated(USER_ID);
            when(service.hasWorkspaceRoleAtLeast(USER_ID, "test-workspace", WorkspaceRole.ADMIN))
                    .thenReturn(false);

            assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                    .isInstanceOf(AuthException.class);
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

        @RequireWorkspacePermission(minRole = WorkspaceRoleCodes.ADMIN)
        public void adminRoute() {
        }
    }
}
