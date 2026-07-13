package com.syncturtle.platform.gateway.filters.global;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.platform.gateway.filters.GatewayExchangeAttributes;
import com.syncturtle.platform.gateway.filters.GatewayFilterOrders;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Slf4j(topic = "access")
@Component
public final class AccessLogFilter implements GlobalFilter, Ordered {

    private static final int MAX_LOG_VALUE_LENGTH = 1_024;

    @Override
    public int getOrder() {
        return GatewayFilterOrders.ACCESS_LOG;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).doOnError(exception -> {
            exchange.getAttributes().put(GatewayExchangeAttributes.FAILURE, exception);
        }).doFinally(signalType -> writeAccessLog(exchange, signalType));
    }

    private static void writeAccessLog(ServerWebExchange exchange, SignalType signalType) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders requestHeaders = request.getHeaders();
        Throwable failure = exchange.getAttribute(GatewayExchangeAttributes.FAILURE);
        int status = resolveStatus(response, failure, signalType);

        String correlationId = firstNonBlank(
                response.getHeaders().getFirst(GatewayHeaders.HDR_CORRELATION_ID),
                requestHeaders.getFirst(GatewayHeaders.HDR_CORRELATION_ID));

        String requestId = firstNonBlank(
                response.getHeaders().getFirst(GatewayHeaders.HDR_REQUEST_ID),
                requestHeaders.getFirst(GatewayHeaders.HDR_REQUEST_ID));

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        URI target = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();

        try {
            populateMdc(correlationId, requestId);

            log.info(
                    "method={} path={} status={} durationMs={} "
                            + "route={} target={} "
                            + "userId={} workspaceId={} "
                            + "ip={} ua=\"{}\" "
                            + "requestId={} correlationId={} "
                            + "signal={} exception={}",
                    logValue(request.getMethod().name()),
                    logValue(request.getURI().getPath()),
                    status,
                    durationMillis(exchange),
                    route == null ? "-" : logValue(route.getId()),
                    logValue(safeTarget(target)),
                    logValue(requestHeaders.getFirst(GatewayHeaders.HDR_AUTH_USER_ID)),
                    logValue(requestHeaders.getFirst(GatewayHeaders.HDR_AUTH_WORKSPACE_ID)),
                    logValue(requestHeaders.getFirst(GatewayHeaders.HDR_CLIENT_IP)),
                    logValue(requestHeaders.getFirst(GatewayHeaders.HDR_CLIENT_UA)),
                    logValue(requestId),
                    logValue(correlationId),
                    signalType.name(),
                    failure == null ? "-" : failure.getClass().getName());
        } finally {
            restoreMdc(previousMdc);
        }
    }

    private static long durationMillis(ServerWebExchange exchange) {
        Long startNanos = exchange.getAttribute(GatewayExchangeAttributes.REQUEST_START_NANOS);

        if (startNanos == null) {
            return 0L;
        }

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static int resolveStatus(ServerHttpResponse response, Throwable failure, SignalType signalType) {
        if (response.getStatusCode() != null) {
            return response.getStatusCode().value();
        }

        if (failure != null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        if (signalType == SignalType.CANCEL) {
            return 499;
        }

        return HttpStatus.OK.value();
    }

    private static void populateMdc(String correlationId, String requestId) {
        SpanContext spanContext = Span.current().getSpanContext();

        if (spanContext.isValid()) {
            MDC.put("traceId", spanContext.getTraceId());
            MDC.put("spanId", spanContext.getSpanId());
        }

        if (hasText(correlationId)) {
            MDC.put(GatewayHeaders.HDR_CORRELATION_ID, correlationId);
        }

        if (hasText(requestId)) {
            MDC.put(GatewayHeaders.HDR_REQUEST_ID, requestId);
        }
    }

    private static void restoreMdc(Map<String, String> previousMdc) {
        MDC.clear();

        if (previousMdc != null && !previousMdc.isEmpty()) {
            MDC.setContextMap(previousMdc);
        }
    }

    private static String safeTarget(URI uri) {
        if (uri == null) {
            return null;
        }

        StringBuilder value = new StringBuilder();

        if (uri.getScheme() != null) {
            value.append(uri.getScheme()).append("://");
        }

        if (uri.getHost() != null) {
            value.append(uri.getHost());
        }

        if (uri.getPort() >= 0) {
            value.append(':').append(uri.getPort());
        }

        if (uri.getPath() != null) {
            value.append(uri.getPath());
        }

        return value.toString();

    }

    private static String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }

        if (hasText(second)) {
            return second;
        }

        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String logValue(String value) {
        if (!hasText(value)) {
            return "-";
        }

        String sanitized = value
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('\t', ' ');

        if (sanitized.length() <= MAX_LOG_VALUE_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(0, MAX_LOG_VALUE_LENGTH);
    }

}
