package com.syncturtle.platform.infra.gateway.controllers.fallback;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import io.opentelemetry.api.trace.Span;
import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    @GetMapping(path = "/__fallback/instance-service")
    public Mono<ResponseEntity<Map<String, Object>>> instanceServiceFallback(ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        String traceId = null;
        Span span = Span.current();
        if (span != null && span.getSpanContext().isValid()) {
            traceId = span.getSpanContext().getTraceId();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", "DOWNSTREAM_UNAVAILABLE");
        body.put("service", "instance-service");
        body.put("message", "Temporarily unavailable");
        if (traceId != null) {
            body.put("traceId", traceId);
        }
        if (cause != null) {
            body.put("cause", cause.getClass().getSimpleName());
        }

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
