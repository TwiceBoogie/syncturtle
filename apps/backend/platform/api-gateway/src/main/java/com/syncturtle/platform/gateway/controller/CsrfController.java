package com.syncturtle.platform.gateway.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.security.cookie.ReactiveCsrfCookieWriter;
import com.syncturtle.platform.gateway.dto.response.CsrfTokenResponse;
import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.IssuedPreAuthCsrfToken;
import com.syncturtle.platform.gateway.security.csrf.IssuedSessionCsrfToken;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;
import com.syncturtle.platform.gateway.type.GatewayCsrfScope;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
public class CsrfController {

    private final GatewayCsrfTokenProcessor csrfTokenProcessor;
    private final ReactiveCsrfCookieWriter csrfCookieWriter;
    private final Clock clock;

    @GetMapping("/api/get-csrf-token")
    public Mono<ResponseEntity<CsrfTokenResponse>> getCsrfToken(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .ofType(PassportAuthenticationToken.class)
                .map(authentication -> issueSession(exchange, authentication.getValidatedSession()))
                .switchIfEmpty(Mono.fromSupplier(() -> issuePreAuth(exchange)));
    }

    private ResponseEntity<CsrfTokenResponse> issuePreAuth(ServerWebExchange exchange) {
        IssuedPreAuthCsrfToken issued = csrfTokenProcessor.issuePreAuth();
        csrfCookieWriter.setCsrfCookie(
                exchange.getResponse(),
                issued.getSignedCookieToken(),
                remainingAge(issued.getExpiresAt()));
        CsrfTokenResponse response = new CsrfTokenResponse(
                issued.getSubmittedToken(),
                GatewayCsrfScope.PREAUTH,
                issued.getExpiresAt());
        return noStore(response);
    }

    private ResponseEntity<CsrfTokenResponse> issueSession(
            ServerWebExchange exchange,
            ParsedPassportSession session) {
        IssuedSessionCsrfToken issued = csrfTokenProcessor.issueSession(session.getSessionId(),
                session.getExpiresAt());
        csrfCookieWriter.setCsrfCookie(
                exchange.getResponse(),
                issued.getSignedCookieToken(),
                remainingAge(issued.getExpiresAt()));
        CsrfTokenResponse response = new CsrfTokenResponse(
                issued.getSubmittedToken(),
                GatewayCsrfScope.SESSION,
                issued.getExpiresAt());
        return noStore(response);
    }

    private Duration remainingAge(Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(clock), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private static ResponseEntity<CsrfTokenResponse> noStore(CsrfTokenResponse response) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

}
