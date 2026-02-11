package com.syncturtle.platform.services.instance.configurations.web.filters;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.properties.CsrfTransportProperties;
import com.syncturtle.common.spring.web.url.ServletHostUrlBuilder;
import com.syncturtle.common.web.security.csrf.CsrfTokenSigner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public final class FormCsrfOncePerRequestFilter extends OncePerRequestFilter {

    private final CsrfTokenSigner csrfTokenSigner;
    private final CsrfTransportProperties csrfTransportProps;
    private final ServletHostUrlBuilder hostResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String formToken = request.getParameter(csrfTransportProps.getFormFieldName());
        String cookieToken = readCookie(request, csrfTransportProps.getCookieName());

        if (!StringUtils.hasText(formToken) || !StringUtils.hasText(cookieToken)) {
            AuthenticationException exception = AuthenticationException.of(AuthErrorCode.INVALID_CSRF_TOKEN);
            String location = hostResolver.buildAdminRedirectUrlWithErrors(request, exception.getErrorMap());
            deny(response, location);
            return;
        }

        if (!csrfTokenSigner.verify(formToken) || !csrfTokenSigner.verify(cookieToken)) {
            AuthenticationException exception = AuthenticationException.of(AuthErrorCode.INVALID_CSRF_TOKEN);
            String location = hostResolver.buildAdminRedirectUrlWithErrors(request, exception.getErrorMap());
            deny(response, location);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!"/api/instances/admins/sign-up".equals(request.getRequestURI())) {
            return true;
        }

        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return true;
        }
        return !contentType.toLowerCase().startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void deny(HttpServletResponse response, String location) throws IOException {
        response.setStatus(303);
        response.setHeader("Location", location);
    }

}
