package com.syncturtle.services.workspace.configuration.web.authorization;

import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.security.authorization.AuthorizationDecision;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;

public final class WorkspaceAuthorizationInterceptor extends AbstractAuthorizationInterceptor {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public WorkspaceAuthorizationInterceptor(RequestUserContext requestUserContext,
            WorkspaceAuthorizationService workspaceAuthorizationService, Boolean requireAuthenticationByDefault) {
        super(requestUserContext, requireAuthenticationByDefault);
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Override
    protected AuthorizationDecision authorizeDomain(HttpServletRequest request, HandlerMethod handlerMethod) {
        RequireWorkspacePermission annotation = findMethodOrClassAnnotation(handlerMethod,
                RequireWorkspacePermission.class);

        if (annotation == null) {
            return AuthorizationDecision.abstain();
        }

        requireAuthenticated();

        String workspaceSlug = resolveWorkspaceSlug(request);

        WorkspaceRole requiredRole = WorkspaceRole.fromCode(annotation.minRole())
                .orElseThrow(() -> new IllegalStateException(
                        "@RequireWorkspacePermission minRole does not map to a known WorkspaceRole: "
                                + annotation.minRole()));

        boolean allowed = workspaceAuthorizationService.hasWorkspaceRoleAtLeast(requestUserContext().requireUserId(),
                workspaceSlug, requiredRole);

        if (!allowed) {
            return AuthorizationDecision.deny(AuthErrorCode.INSUFFICIENT_WORKSPACE_ROLE);
        }

        return AuthorizationDecision.allow();
    }

    private static String resolveWorkspaceSlug(HttpServletRequest request) {
        Object value = request.getAttribute("slug");

        if (value == null) {
            value = request.getAttribute("workspaceSlug");
        }

        String slug = value == null ? null : String.valueOf(value);

        if (!StringUtils.hasText(slug)) {
            throw new IllegalStateException("Workspace slug path variable is required");
        }

        return slug;
    }

}
