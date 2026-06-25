package com.syncturtle.services.user.configuration.web.filter;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_FORM_FIELD_NAME;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.web.url.PublicUrlResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class FormCsrfOncePerRequestFilter extends OncePerRequestFilter {

    private final Set<String> protectedFormEndpoints;
    private final CsrfTokenService csrfTokenService;
    private final ServletAuthCookieWriter cookieWriter;
    private final PublicUrlResolver hostResolver;

    public FormCsrfOncePerRequestFilter(
            Collection<String> protectedFormEndpoints,
            CsrfTokenService csrfTokenService,
            ServletAuthCookieWriter cookieWriter,
            PublicUrlResolver hostResolver) {
        Assert.notEmpty(protectedFormEndpoints, "protectedFormEndpoints must not be empty");

        this.protectedFormEndpoints = Set.copyOf(protectedFormEndpoints);
        this.csrfTokenService = csrfTokenService;
        this.cookieWriter = cookieWriter;
        this.hostResolver = hostResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!hasValidCsrfPair(request)) {
            denyWithCsrfError(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedPostFormEndpoint(request);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    private boolean isProtectedPostFormEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getServletPath();
        if (!StringUtils.hasText(path)) {
            path = request.getRequestURI();
        }

        if (!protectedFormEndpoints.contains(path)) {
            return false;
        }

        return isApplicationFormUrlEncoded(request);
    }

    private boolean isApplicationFormUrlEncoded(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        try {
            MediaType actualContentType = MediaType.parseMediaType(contentType);
            return actualContentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED);
        } catch (InvalidMediaTypeException exception) {
            return false;
        }
    }

    private boolean hasValidCsrfPair(HttpServletRequest request) {
        String signedToken = readCookie(request, cookieWriter.csrfCookieName());
        String submittedRawToken = request.getParameter(CSRF_FORM_FIELD_NAME);

        return csrfTokenService.matches(signedToken, submittedRawToken);
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

    private void denyWithCsrfError(HttpServletResponse response) throws IOException {
        AuthException exception = AuthException.of(AuthErrorCode.INVALID_CSRF_TOKEN);
        cookieWriter.clearCsrfCookie(response);

        String location = hostResolver.userAppWithQuery("", exception.getErrorMap());

        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.LOCATION, response.encodeRedirectURL(location));
    }

}
