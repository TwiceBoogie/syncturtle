package com.syncturtle.platform.gateway.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.spring.properties.CsrfCookiePolicyProperties;
import com.syncturtle.common.spring.properties.CsrfTransportProperties;
import com.syncturtle.common.web.csrf.CsrfTokenSigner;
import com.syncturtle.common.web.csrf.HmacCsrfTokenSigner;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
public class CsrfController {

    private final CsrfTransportProperties csrfTransportProps;
    private final CsrfCookiePolicyProperties cookiePolicyProps;
    private final CsrfTokenSigner csrfTokenSigner;

    @GetMapping("/api/get-csrf-token")
    public ResponseEntity<Map<String, Object>> getCsrfToken(ServerHttpResponse response) {
        String raw = HmacCsrfTokenSigner.generateRawToken(32);
        String signed = csrfTokenSigner.sign(raw);

        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(csrfTransportProps.getCookieName(), signed)
                .httpOnly(cookiePolicyProps.isHttpOnly())
                .secure(cookiePolicyProps.isSecure())
                .sameSite(cookiePolicyProps.getSameSite())
                .path(cookiePolicyProps.getPath())
                .maxAge(cookiePolicyProps.getMaxAge());

        if (StringUtils.hasText(cookiePolicyProps.getDomain())) {
            cookie.domain(cookiePolicyProps.getDomain());
        }

        response.addCookie(cookie.build());

        return ResponseEntity.ok(
                Map.of("ok", true, "csrfToken", raw, "cookieName", csrfTransportProps.getCookieName(), "headerName",
                        csrfTransportProps.getHeaderName()));
    }

}
