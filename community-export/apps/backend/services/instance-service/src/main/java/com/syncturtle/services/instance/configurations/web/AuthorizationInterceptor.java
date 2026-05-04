package com.syncturtle.services.instance.configurations.web;

import java.lang.annotation.Annotation;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

@Profile("!setup")
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final RequestUserContext ctx;
    private final InstanceAuthorizationService instanceAuthz;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        // 1: AllowAnonymous win
        if (findAnnotation(hm, AllowAnonymous.class) != null) {
            return true;
        }

        // 2: Instance admin requirements
        RequireInstanceAdmin admin = findAnnotation(hm, RequireInstanceAdmin.class);
        if (admin != null) {
            if (!ctx.isAuthenticated()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"details\":\"Authentication credentials were not provided\"}");
                return false;
            }
            if (!instanceAuthz.isInstanceAdmin(ctx.getUserId(), admin.minRole())) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return false;
            }
            return true;
        }

        // 3: Authenticated requirements
        if (findAnnotation(hm, RequireAuthenticated.class) != null) {
            if (!ctx.isAuthenticated()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            }
        }

        return true;
    }

    private <A extends Annotation> A findAnnotation(HandlerMethod hm, Class<A> annType) {
        // method first
        A onMethod = AnnotationUtils.findAnnotation(hm.getMethod(), annType);
        if (onMethod != null) {
            return onMethod;
        }
        // then class
        return AnnotationUtils.findAnnotation(hm.getBeanType(), annType);
    }
}
