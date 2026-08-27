package com.syncturtle.services.user.controller;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_SESSION_ID;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.user.dto.response.UserSessionInventoryResponse;
import com.syncturtle.services.user.dto.response.UserSessionRevocationResponse;
import com.syncturtle.services.user.service.UserSessionService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/sessions")
public class UserSessionController {

    private final UserSessionService service;
    private final ServletAuthCookieWriter cookieWriter;

    @GetMapping
    public ResponseEntity<UserSessionInventoryResponse> listSessions(
            @CurrentUser UUID currentUserId,
            @RequestHeader(HDR_AUTH_SESSION_ID) UUID currentSessionId) {
        UserSessionInventoryResponse response = service.listSessions(currentUserId, currentSessionId);
        return noStore(response);
    }

    @DeleteMapping("/others")
    public ResponseEntity<UserSessionRevocationResponse> revokeOtherSessions(
            @CurrentUser UUID currentUserId,
            @RequestHeader(HDR_AUTH_SESSION_ID) UUID currentSessionId,
            HttpServletResponse servletResponse) {
        UserSessionRevocationResponse response = service.revokeOtherSessions(currentUserId, currentSessionId);
        clearCookiesWhenCurrentWasRevoked(response, servletResponse);
        return noStore(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<UserSessionRevocationResponse> revokeSession(
            @CurrentUser UUID currentUserId,
            @RequestHeader(HDR_AUTH_SESSION_ID) UUID currentSessionId,
            @PathVariable UUID sessionId,
            HttpServletResponse servletResponse) {
        UserSessionRevocationResponse response = service.revokeSession(currentUserId, currentSessionId, sessionId);
        clearCookiesWhenCurrentWasRevoked(response, servletResponse);
        return noStore(response);
    }

    @DeleteMapping
    public ResponseEntity<UserSessionRevocationResponse> revokeAllSessions(
            @CurrentUser UUID currentUserId,
            @RequestHeader(HDR_AUTH_SESSION_ID) UUID currentSessionId,
            HttpServletResponse servletResponse) {
        UserSessionRevocationResponse response = service.revokeAllSessions(currentUserId, currentSessionId);
        clearCookiesWhenCurrentWasRevoked(response, servletResponse);
        return noStore(response);
    }

    private void clearCookiesWhenCurrentWasRevoked(
            UserSessionRevocationResponse response,
            HttpServletResponse servletResponse) {
        if (response.isCurrentSessionRevoked()) {
            cookieWriter.clearAllSecurityCookies(servletResponse);
        }
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

}
