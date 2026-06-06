package com.syncturtle.platform.gateway.controllers;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_HEADER_NAME;
import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_FORM_FIELD_NAME;

import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.security.cookie.ReactiveCsrfCookieWriter;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.security.csrf.IssuedCsrfToken;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
public class CsrfController {

    private final CsrfTokenService csrfTokenService;
    private final ReactiveCsrfCookieWriter csrfCookieWriter;

    @GetMapping("/api/get-csrf-token")
    public ResponseEntity<Map<String, Object>> getCsrfToken(ServerHttpResponse response) {
        IssuedCsrfToken token = csrfTokenService.issueToken();

        csrfCookieWriter.setCsrfCookie(response, token.getSignedToken());

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "csrfToken", token.getRawToken(),
                "cookieName", csrfCookieWriter.csrfCookieName(),
                "headerName", CSRF_HEADER_NAME,
                "formFieldName", CSRF_FORM_FIELD_NAME));
    }

}
