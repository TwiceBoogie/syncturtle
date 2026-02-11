package com.syncturtle.platform.services.instance.configurations.web.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Profile;
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
@Profile("!setup")
@Component
@RequiredArgsConstructor
public final class FormCsrfOncePerRequestFilter extends OncePerRequestFilter {

    private static final List<String> ENDPOINTS = List.of("/api/instances/admins/sign-up",
            "/api/instances/admins/sign-in");

    private final CsrfTokenSigner csrfTokenSigner;
    private final CsrfTransportProperties csrfTransportProps;
    private final ServletHostUrlBuilder hostResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawFormToken = request.getParameter(csrfTransportProps.getFormFieldName());
        String signedCookie = readCookie(request, csrfTransportProps.getCookieName());

        if (!StringUtils.hasText(rawFormToken) || !StringUtils.hasText(signedCookie)) {
            denyWithCsrfError(request, response);
            return;
        }

        // 1: cookie must be a valid signed token
        if (!csrfTokenSigner.verify(signedCookie)) {
            denyWithCsrfError(request, response);
            return;
        }

        // 2: compare raw form token to raw token inside cookie
        String rawCookieToken = csrfTokenSigner.extractToken(signedCookie);
        if (!StringUtils.hasText(rawCookieToken) || !constantTimeEquals(rawCookieToken, rawFormToken.trim())) {
            denyWithCsrfError(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!ENDPOINTS.contains(request.getRequestURI())) {
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

    private void denyWithCsrfError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthenticationException exception = AuthenticationException.of(AuthErrorCode.INVALID_CSRF_TOKEN);
        String location = hostResolver.buildAdminRedirectUrlWithErrors(request, exception.getErrorMap());
        deny(response, location);
    }

    private void deny(HttpServletResponse response, String location) throws IOException {
        response.setStatus(303);
        response.setHeader("Location", location);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);

        int diff = x.length ^ y.length;
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            diff |= x[i] ^ y[i];
        }
        return diff == 0;
    }

}
