package com.syncturtle.common.web.mvc;

import java.util.Objects;
import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.common.web.context.RequestUser;
import com.syncturtle.common.web.context.RequestUserContext;

public final class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final RequestUserContext requestUserContext;

    public CurrentUserArgumentResolver(RequestUserContext requestUserContext) {
        this.requestUserContext = Objects.requireNonNull(requestUserContext, "requestUserContext is required");
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (annotation == null) {
            throw new IllegalStateException("@CurrentUser annotation is required.");
        }

        RequestUser currentUser = requestUserContext.getCurrentUser();
        Class<?> parameterType = parameter.getParameterType();
        if (!currentUser.isAuthenticated()) {
            return resolveAnonymousUser(annotation, parameterType);
        }

        if (UUID.class.equals(parameterType)) {
            return currentUser.requireUserId();
        }

        if (RequestUser.class.equals(parameterType)) {
            return currentUser;
        }

        throw unsupportedParameterType(parameter);
    }

    private static Object resolveAnonymousUser(CurrentUser annotation, Class<?> parameterType) {
        if (RequestUser.class.equals(parameterType) && !annotation.required()) {
            return RequestUser.anonymous();
        }

        if (UUID.class.equals(parameterType) && !annotation.required()) {
            return null;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }

    private static IllegalArgumentException unsupportedParameterType(MethodParameter parameter) {
        return new IllegalArgumentException("@CurrentUser only supports UUID or RequestUser parameters: "
                + parameter.getExecutable().toGenericString());
    }

}
