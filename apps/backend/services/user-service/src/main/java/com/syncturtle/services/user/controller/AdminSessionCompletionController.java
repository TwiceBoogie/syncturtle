package com.syncturtle.services.user.controller;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.service.AdminSessionCompletionService;
import com.syncturtle.services.user.service.param.AdminSessionCompletionParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/admin")
public class AdminSessionCompletionController {

    private final AdminSessionCompletionService service;
    private final ServletAuthCookieWriter cookieWriter;
    private final RequestClientContext requestClientContext;
    private final PublicUrlResolver hostResolver;

    @GetMapping("/session")
    public ResponseEntity<Void> complete(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String completionCode = readSingleCookie(servletRequest, cookieWriter.adminSessionHandoffCookieName());
        String signedCsrfToken = readSingleCookie(servletRequest, cookieWriter.csrfCookieName());

        if (!StringUtils.hasText(completionCode) || !StringUtils.hasText(signedCsrfToken)) {
            cookieWriter.clearAdminSessionHandoffCookie(servletResponse);
            return redirect(hostResolver.admin(""));
        }

        try {
            IssueTokenResponse response = service.complete(AdminSessionCompletionParam.builder()
                    .completionCode(completionCode)
                    .signedCsrfToken(signedCsrfToken)
                    .clientIp(requestClientContext.getClientIp())
                    .userAgent(requestClientContext.getUserAgent())
                    .build());
            writeSessionCookies(servletResponse, response);
            cookieWriter.clearCsrfCookie(servletResponse);
            cookieWriter.clearAdminSessionHandoffCookie(servletResponse);

            return redirect(response.getLocation());
        } catch (AdminSessionHandoffException exception) {
            if (exception.getReason() == AdminSessionHandoffException.Reason.REDIS_UNAVAILABLE) {
                throw exception;
            }

            cookieWriter.clearAdminSessionHandoffCookie(servletResponse);
            return redirect(hostResolver.admin(""));
        } catch (IllegalArgumentException exception) {
            cookieWriter.clearAdminSessionHandoffCookie(servletResponse);

            return redirect(hostResolver.admin(""));
        }
    }

    private void writeSessionCookies(HttpServletResponse servletResponse, IssueTokenResponse response) {
        Duration accessMaxAge = Duration.between(response.getAccessIssuedAt(), response.getAccessExpiresAt());
        Duration refreshMaxAge = Duration.between(response.getRefreshIssuedAt(), response.getRefreshExpiresAt());

        cookieWriter.setAuthCookies(
                servletResponse,
                response.getAccessToken(),
                accessMaxAge,
                response.getRefreshToken(),
                refreshMaxAge);
    }

    private static ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(location))
                .build();
    }

    private static String readSingleCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        String found = null;
        for (Cookie cookie : cookies) {
            if (!name.equals(cookie.getName())) {
                continue;
            }

            if (found != null) {
                return null;
            }

            found = cookie.getValue();
        }

        return found;
    }

}
