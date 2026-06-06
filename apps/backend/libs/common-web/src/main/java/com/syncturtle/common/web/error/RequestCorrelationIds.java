package com.syncturtle.common.web.error;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.header.GatewayHeaders;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestCorrelationIds {

    private RequestCorrelationIds() {
    }

    public static String resolveTraceId(HttpServletRequest request) {
        return firstNonBlank(
                header(request, "traceparent", RequestCorrelationIds::traceIdFromTraceparent),
                header(request, "X-B3-TraceId"),
                header(request, "X-Trace-Id"),
                MDC.get("traceId"),
                MDC.get("trace_id"),
                MDC.get("otelTraceId"));
    }

    public static String resolveRequestId(HttpServletRequest request) {
        return firstNonBlank(
                header(request, GatewayHeaders.HDR_REQUEST_ID),
                MDC.get(GatewayHeaders.HDR_REQUEST_ID),
                MDC.get("requestId"),
                MDC.get("request_id"));
    }

    public static String resolveCorrelationId(HttpServletRequest request) {
        return firstNonBlank(
                header(request, GatewayHeaders.HDR_CORRELATION_ID),
                MDC.get(GatewayHeaders.HDR_CORRELATION_ID),
                MDC.get("correlationId"),
                MDC.get("correlation_id"));
    }

    private static String header(HttpServletRequest request, String name) {
        if (request == null) {
            return null;
        }

        String value = request.getHeader(name);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String header(HttpServletRequest request, String name, HeaderValueMapper mapper) {
        String value = header(request, name);
        return value == null ? null : mapper.map(value);
    }

    private static String traceIdFromTraceparent(String traceparent) {
        // W3C traceparent format:
        // version-traceid-spanid-flags
        // Example:
        // 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01

        if (!StringUtils.hasText(traceparent)) {
            return null;
        }

        String[] parts = traceparent.trim().split("-");
        if (parts.length < 4) {
            return null;
        }

        String traceId = parts[1];
        if (traceId.length() != 32) {
            return null;
        }

        if (!traceId.matches("[0-9a-fA-F]{32}")) {
            return null;
        }

        return traceId.toLowerCase();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }

    @FunctionalInterface
    private interface HeaderValueMapper {
        String map(String value);
    }

}
