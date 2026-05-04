package com.syncturtle.services.user.configurations.web.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.spring.properties.CsrfTransportProperties;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;
import com.syncturtle.common.web.csrf.CsrfTokenSigner;

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

    private static final List<String> ENDPOINTS = List.of("/auth/sign-out");

    private final CsrfTokenSigner signer;
    private final CsrfTransportProperties props;
    private final PublicUrlBuilder hostResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawFormToken = request.getParameter(props.getFormFieldName());
        String signedCookie = readCookie(request, props.getCookieName());

        if (!StringUtils.hasText(rawFormToken) || !StringUtils.hasText(signedCookie)) {
            denyWithCsrfError(request, response);
            return;
        }

        if (!signer.verify(signedCookie)) {
            denyWithCsrfError(request, response);
            return;
        }

        String rawCookieToken = signer.extractToken(signedCookie);
        if (!StringUtils.hasText(rawCookieToken) || !constantTimeEquals(rawCookieToken, rawFormToken.trim())) {
            denyWithCsrfError(request, response);
            return;
        }

        filterChain.doFilter(request, response);
        ;
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
        AuthException exception = AuthException.of(AuthErrorCode.INVALID_CSRF_TOKEN);
        String location = hostResolver.adminWithQuery("", exception.getErrorMap());
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
