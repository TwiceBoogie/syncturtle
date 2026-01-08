package com.syncturtle.platform.infra.gateway.filters.global;

import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.constants.GatewayHeaderNames;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j(topic = "access")
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    URI target = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    Throwable cbEx = exchange
                            .getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
                    log.info("target={} cbEx={} cbMsg={}", target, (cbEx != null ? cbEx.getClass().getName() : "-"),
                            (cbEx != null ? cbEx.getMessage() : "-"));

                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

                    ServerHttpRequest request = exchange.getRequest();
                    ServerHttpResponse response = exchange.getResponse();

                    int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
                    String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
                    String path = request.getURI().getPath();

                    HttpHeaders reqHeaders = request.getHeaders();
                    String ua = reqHeaders.getFirst("User-Agent");
                    String ip = resolveClientIp(request);

                    String userId = reqHeaders.getFirst(GatewayHeaderNames.HDR_AUTH_USER_ID);
                    String workspaceId = reqHeaders.getFirst(GatewayHeaderNames.HDR_AUTH_WORKSPACE_ID);

                    String corrId = firstNonBlank(
                            response.getHeaders().getFirst(GatewayHeaderNames.HDR_CORRELATION_ID),
                            reqHeaders.getFirst(GatewayHeaderNames.HDR_CORRELATION_ID));
                    String reqId = firstNonBlank(
                            response.getHeaders().getFirst(GatewayHeaderNames.HDR_REQUEST_ID),
                            reqHeaders.getFirst(GatewayHeaderNames.HDR_REQUEST_ID));

                    try {
                        putMdcForOneLogLine(corrId, reqId);
                        log.info("{} {} {} {}ms userId={} workspaceId={} ip={} ua=\"{}\" reqId={} corrId={}", method,
                                path,
                                status, durationMs, nullToDash(userId), nullToDash(workspaceId), nullToDash(ip),
                                nullToDash(ua),
                                nullToDash(reqId), nullToDash(corrId));

                    } finally {
                        clearMdcForOneLogLine();
                    }
                });
    }

    private static void putMdcForOneLogLine(String corrId, String reqId) {
        if (MDC.get("traceId") == null || MDC.get("spanId") == null) {
            Span span = Span.current();
            if (span != null && span.getSpanContext().isValid()) {
                MDC.put("traceId", span.getSpanContext().getTraceId());
                MDC.put("spanId", span.getSpanContext().getSpanId());
            }
        }
        if (corrId != null) {
            MDC.put("X-Correlation-Id", corrId);
        }
        if (reqId != null) {
            MDC.put("X-Request-Id", reqId);
        }
    }

    private static void clearMdcForOneLogLine() {
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("X-Correlation-Id");
        MDC.remove("X-Request-Id");
    }

    private static String resolveClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String xff = headers.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xReal = headers.getFirst("X-Real-Ip");
        if (xReal != null && !xReal.isBlank()) {
            return xReal.trim();
        }

        return Optional.ofNullable(request.getRemoteAddress())
                .map(addr -> addr.getAddress())
                .map(InetAddress::getHostAddress)
                .orElse("unknown");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

}
