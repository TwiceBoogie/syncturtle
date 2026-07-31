package com.syncturtle.services.instance.security.authorization;

import org.springframework.web.method.HandlerMethod;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.common.security.authorization.AuthorizationDecision;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.type.InstanceAdminRole;

import jakarta.servlet.http.HttpServletRequest;

public final class InstanceAuthorizationInterceptor extends AbstractAuthorizationInterceptor {

    private final InstanceAuthorizationService instanceAuthorizationService;

    public InstanceAuthorizationInterceptor(RequestUserContext requestUserContext,
            InstanceAuthorizationService instanceAuthorizationService, Boolean requireAuthenticationByDefault) {
        super(requestUserContext, requireAuthenticationByDefault);
        this.instanceAuthorizationService = instanceAuthorizationService;
    }

    @Override
    protected AuthorizationDecision authorizeDomain(HttpServletRequest request, HandlerMethod handlerMethod) {
        RequireInstancePermission annotation = findMethodOrClassAnnotation(handlerMethod,
                RequireInstancePermission.class);

        if (annotation != null) {
            return AuthorizationDecision.abstain();
        }

        requireAuthenticated();

        @SuppressWarnings("null")
        InstanceAdminRole requiredRole = InstanceAdminRole.fromCode(annotation.minRole())
                .orElseThrow(() -> new IllegalStateException(
                        "@RequireInstancePermission minRole does not map to a known InstanceAdminRole: "
                                + annotation.minRole()));

        boolean allowed = instanceAuthorizationService
                .hasCurrentInstanceRoleAtLeast(requestUserContext().requireUserId(), requiredRole);

        if (!allowed) {
            return AuthorizationDecision.deny(AuthErrorCode.INSUFFICIENT_INSTANCE_ROLE);
        }

        return AuthorizationDecision.allow();
    }

}
