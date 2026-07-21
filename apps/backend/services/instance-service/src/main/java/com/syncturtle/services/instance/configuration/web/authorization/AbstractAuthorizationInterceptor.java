package com.syncturtle.services.instance.configuration.web.authorization;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.security.annotation.RequireAuthentication;
import com.syncturtle.common.security.authorization.AuthorizationDecision;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractAuthorizationInterceptor implements HandlerInterceptor {

    private final RequestUserContext requestUserContext;
    private final boolean requireAuthenticationByDefault;

    protected AbstractAuthorizationInterceptor(RequestUserContext requestUserContext,
            Boolean requireAuthenticationByDefault) {
        Assert.notNull(requestUserContext, "requestUserContext is required");
        Assert.notNull(requireAuthenticationByDefault, "requireAuthenticationByDefault is required");

        this.requestUserContext = requestUserContext;
        this.requireAuthenticationByDefault = requireAuthenticationByDefault;
    }

    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        validateGeneralAnnotationCobination(handlerMethod);

        if (allowsAnonymous(handlerMethod)) {
            return true;
        }

        AuthorizationDecision domainDecision = authorizeDomain(request, handlerMethod);

        if (domainDecision.isAllow()) {
            return true;
        }

        if (domainDecision.isDenied()) {
            throw domainDecision.toException();
        }

        if (requiresAuthentication(handlerMethod)) {
            requireAuthenticated();
        }

        return true;
    }

    protected AuthorizationDecision authorizeDomain(HttpServletRequest request, HandlerMethod handlerMethod) {
        return AuthorizationDecision.abstain();
    }

    protected final RequestUserContext requestUserContext() {
        return requestUserContext;
    }

    protected final void requireAuthenticated() {
        if (!requestUserContext.isAuthenticated()) {
            throw AuthException.of(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private boolean allowsAnonymous(HandlerMethod handlerMethod) {
        return findMethodOrClassAnnotation(handlerMethod, AllowAnonymous.class) != null;
    }

    private boolean requiresAuthentication(HandlerMethod handlerMethod) {
        return requireAuthenticationByDefault
                || findMethodOrClassAnnotation(handlerMethod, RequireAuthentication.class) != null;
    }

    private static void validateGeneralAnnotationCobination(HandlerMethod handlerMethod) {
        AllowAnonymous allowAnonymous = findMethodOrClassAnnotation(handlerMethod, AllowAnonymous.class);
        RequireAuthentication requireAuthentication = findMethodOrClassAnnotation(handlerMethod,
                RequireAuthentication.class);

        if (allowAnonymous != null && requireAuthentication != null) {
            throw new IllegalStateException("Only one of @AllowAnonymous or @RequireAuthentication is allowed on "
                    + handlerMethod.getMethod().toGenericString());
        }
    }

    protected static <A extends Annotation> A findMethodOrClassAnnotation(HandlerMethod handlerMethod,
            Class<A> annotationType) {
        A methodAnnotation = findAnnotation(handlerMethod.getMethod(), annotationType);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return findAnnotation(handlerMethod.getBeanType(), annotationType);
    }

    private static <A extends Annotation> A findAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(element, annotationType);
    }

}
