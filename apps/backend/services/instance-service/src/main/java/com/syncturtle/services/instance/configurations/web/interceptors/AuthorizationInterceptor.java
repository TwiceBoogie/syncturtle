package com.syncturtle.services.instance.configurations.web.interceptors;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireAuthenticated;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.services.authz.InstanceAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AuthorizationInterceptor implements HandlerInterceptor {

    private final RequestUserContext requestUserContext;
    private final InstanceAuthorizationService instanceAuthorizationService;
    private final AuthorizationResponseWriter responseWriter;
    private final boolean requireAuthenticationByDefault;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 1: AllowAnonymous win
        if (hasAnnotation(handlerMethod, AllowAnonymous.class)) {
            return true;
        }

        // 2: Instance admin requirements
        RequireInstanceAdmin requireInstanceAdmin = findAnnotation(handlerMethod, RequireInstanceAdmin.class);
        if (requireInstanceAdmin != null) {
            return handleRequireInstanceAdmin(response, requireInstanceAdmin);
        }

        // 3: Authenticated requirements
        if (hasAnnotation(handlerMethod, RequireAuthenticated.class)) {
            return handleRequireAuthenticated(response);
        }

        if (requireAuthenticationByDefault) {
            return handleRequireAuthenticated(response);
        }

        return true;
    }

    private boolean handleRequireInstanceAdmin(HttpServletResponse response, RequireInstanceAdmin requireInstanceAdmin)
            throws Exception {
        if (!requestUserContext.isAuthenticated()) {
            responseWriter.writeError(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED",
                    "Authentication credentials were not provided.");

            return false;
        }

        boolean allowed = instanceAuthorizationService.hasInstanceRoleAtLeast(
                requestUserContext.getUserId(),
                requireInstanceAdmin.minRole());
        if (!allowed) {
            responseWriter.writeError(
                    response,
                    HttpStatus.FORBIDDEN,
                    "INSUFFICIENT_INSTANCE_ROLE",
                    "You do not have permission to access this resource.");

            return false;
        }

        return true;
    }

    private boolean handleRequireAuthenticated(HttpServletResponse response) throws Exception {
        if (requestUserContext.isAuthenticated()) {
            return true;
        }

        responseWriter.writeError(
                response,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication credentials were not provided.");

        return false;
    }

    private static <A extends Annotation> boolean hasAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        return findAnnotation(handlerMethod, annotationType) != null;
    }

    private static <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        // method first
        A methodAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), annotationType);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        // then class
        return AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), annotationType);
    }

}
