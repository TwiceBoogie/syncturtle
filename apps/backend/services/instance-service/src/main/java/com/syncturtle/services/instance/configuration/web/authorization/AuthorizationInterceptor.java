package com.syncturtle.services.instance.configuration.web.authorization;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.security.annotation.RequireAuthentication;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.type.InstanceAdminRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthorizationInterceptor implements HandlerInterceptor {

    private static final String AUTHENTICATION_REQUIRED_CODE = "AUTHENTICATION_REQUIRED";
    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "Authentication credentials were not provided.";
    private static final String INSUFFICIENT_INSTANCE_ROLE_CODE = "INSUFFICIENT_INSTANCE_ROLE";
    private static final String INSUFFICIENT_INSTANCE_ROLE_MESSAGE = "You do not have permission to access this resource.";

    private final RequestUserContext requestUserContext;
    private final InstanceAuthorizationService instanceAuthorizationService;
    private final AuthorizationResponseWriter responseWriter;
    private final boolean requireAuthenticationByDefault;
    private final Map<HandlerMethod, AuthorizationRule> ruleCache = new ConcurrentHashMap<>();

    public AuthorizationInterceptor(
            RequestUserContext requestUserContext,
            InstanceAuthorizationService instanceAuthorizationService,
            AuthorizationResponseWriter responseWriter,
            boolean requireAuthenticationByDefault) {
        Assert.notNull(requestUserContext, "requestUserContext is required");
        Assert.notNull(instanceAuthorizationService, "instanceAuthorizationService is required");
        Assert.notNull(responseWriter, "responseWriter is required");

        this.requestUserContext = requestUserContext;
        this.instanceAuthorizationService = instanceAuthorizationService;
        this.responseWriter = responseWriter;
        this.requireAuthenticationByDefault = requireAuthenticationByDefault;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AuthorizationRule rule = resolveRule(handlerMethod);
        if (rule.isAnonymousAllowed()) {
            return true;
        }

        if (rule.requiresInstancePermission()) {
            return handleRequireInstancePermission(response, rule.requiredRole());
        }

        if (rule.requiresAuthentication()) {
            return handleRequireAuthenticated(response);
        }

        return true;
    }

    private boolean handleRequireInstancePermission(HttpServletResponse response, InstanceAdminRole requiredRole)
            throws Exception {
        if (!requestUserContext.isAuthenticated()) {
            writeAuthenticationRequired(response);
            return false;
        }

        boolean allowed = instanceAuthorizationService.hasCurrentInstanceRoleAtLeast(requestUserContext.requireUserId(),
                requiredRole);
        if (!allowed) {
            responseWriter.writeError(response, HttpStatus.FORBIDDEN, INSUFFICIENT_INSTANCE_ROLE_CODE,
                    INSUFFICIENT_INSTANCE_ROLE_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean handleRequireAuthenticated(HttpServletResponse response) throws Exception {
        if (requestUserContext.isAuthenticated()) {
            return true;
        }

        writeAuthenticationRequired(response);
        return false;
    }

    private void writeAuthenticationRequired(HttpServletResponse response) throws Exception {
        responseWriter.writeError(response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED_CODE,
                AUTHENTICATION_REQUIRED_MESSAGE);
    }

    private AuthorizationRule resolveRule(HandlerMethod handlerMethod) {
        return ruleCache.computeIfAbsent(handlerMethod, this::createRule);
    }

    private AuthorizationRule createRule(HandlerMethod handlerMethod) {
        AuthorizationRule methodRule = readRule(handlerMethod.getMethod());

        if (methodRule.isDeclared()) {
            return methodRule;
        }

        AuthorizationRule classRule = readRule(handlerMethod.getBeanType());

        if (classRule.isDeclared()) {
            return classRule;
        }

        if (requireAuthenticationByDefault) {
            return AuthorizationRule.requireAuthentication();
        }

        return AuthorizationRule.allowByDefault();
    }

    private static AuthorizationRule readRule(AnnotatedElement element) {
        AllowAnonymous allowAnonymous = findAnnotation(element, AllowAnonymous.class);
        RequireAuthentication requireAuthentication = findAnnotation(element, RequireAuthentication.class);
        RequireInstancePermission requireInstancePermission = findAnnotation(element, RequireInstancePermission.class);

        validateSingleAuthorizationAnnotation(element, allowAnonymous, requireAuthentication,
                requireInstancePermission);

        if (allowAnonymous != null) {
            return AuthorizationRule.allowAnonymous();
        }

        if (requireInstancePermission != null) {
            InstanceAdminRole requiredRole = requireKnownRole(requireInstancePermission.minRole(), element);

            return AuthorizationRule.requireInstancePermission(requiredRole);
        }

        if (requireAuthentication != null) {
            return AuthorizationRule.requireAuthentication();
        }

        return AuthorizationRule.notDeclared();
    }

    private static void validateSingleAuthorizationAnnotation(
            AnnotatedElement element,
            AllowAnonymous allowAnonymous,
            RequireAuthentication requireAuthentication,
            RequireInstancePermission requireInstancePermission) {
        int declaredCount = 0;

        if (allowAnonymous != null) {
            declaredCount++;
        }
        if (requireAuthentication != null) {
            declaredCount++;
        }
        if (requireInstancePermission != null) {
            declaredCount++;
        }

        if (declaredCount > 1) {
            throw new IllegalStateException("Only one authorization annotation is allowed on " + element);
        }
    }

    private static InstanceAdminRole requireKnownRole(int roleCode, AnnotatedElement element) {
        if (roleCode < 0) {
            throw new IllegalStateException(
                    "@RequireInstancePermission minRole must be greater than or equal to 0 on " + element);
        }

        return InstanceAdminRole.fromCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("@RequireInstancePermission minRole " + roleCode
                        + " does not map to a known InstanceAdminRole on " + element));
    }

    private static <A extends Annotation> A findAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(element, annotationType);
    }

    private static final class AuthorizationRule {
        private enum Type {
            NOT_DECLARED,
            ALLOW_BY_DEFAULT,
            ALLOW_ANONYMOUS,
            REQUIRE_AUTHENTICATION,
            REQUIRE_INSTANCE_PERMISSION
        }

        private final Type type;
        private final InstanceAdminRole requiredRole;

        private AuthorizationRule(Type type, InstanceAdminRole requiredRole) {
            Assert.notNull(type, "type is required");

            this.type = type;

            if (type == Type.REQUIRE_INSTANCE_PERMISSION) {
                Assert.notNull(requiredRole, "requiredRole is required");
                this.requiredRole = requiredRole;
            } else {
                if (requiredRole != null) {
                    throw new IllegalArgumentException("requiredRole is only allowed for instance permission rules");
                }

                this.requiredRole = null;
            }
        }

        static AuthorizationRule notDeclared() {
            return new AuthorizationRule(Type.NOT_DECLARED, null);
        }

        static AuthorizationRule allowByDefault() {
            return new AuthorizationRule(Type.ALLOW_BY_DEFAULT, null);
        }

        static AuthorizationRule allowAnonymous() {
            return new AuthorizationRule(Type.ALLOW_ANONYMOUS, null);
        }

        static AuthorizationRule requireAuthentication() {
            return new AuthorizationRule(Type.REQUIRE_AUTHENTICATION, null);
        }

        static AuthorizationRule requireInstancePermission(InstanceAdminRole requiredRole) {
            return new AuthorizationRule(Type.REQUIRE_INSTANCE_PERMISSION, requiredRole);
        }

        boolean isDeclared() {
            return type != Type.NOT_DECLARED;
        }

        boolean isAnonymousAllowed() {
            return type == Type.ALLOW_BY_DEFAULT || type == Type.ALLOW_ANONYMOUS;
        }

        boolean requiresAuthentication() {
            return type == Type.REQUIRE_AUTHENTICATION;
        }

        boolean requiresInstancePermission() {
            return type == Type.REQUIRE_INSTANCE_PERMISSION;
        }

        InstanceAdminRole requiredRole() {
            return requiredRole;
        }
    }

}
