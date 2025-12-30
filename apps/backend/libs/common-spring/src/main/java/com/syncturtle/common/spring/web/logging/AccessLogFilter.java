package com.syncturtle.common.spring.web.logging;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.constants.GatewayHeaderNames;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "access")
public class AccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();

        // if tracing filter is earlier
        Context otelCtxAtEntry = Context.current();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            int status = response.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();

            String ua = request.getHeader("User-Agent");
            String ip = resolveClientIp(request);

            String userId = header(request, GatewayHeaderNames.HDR_AUTH_USER_ID);
            String workspaceId = header(request, GatewayHeaderNames.HDR_AUTH_WORKSPACE_ID);

            String corrId = firstNonBlank(
                    response.getHeader(GatewayHeaderNames.HDR_CORRELATION_ID),
                    request.getHeader(GatewayHeaderNames.HDR_CORRELATION_ID));
            String reqId = firstNonBlank(
                    response.getHeader(GatewayHeaderNames.HDR_REQUEST_ID),
                    request.getHeader(GatewayHeaderNames.HDR_REQUEST_ID));

            // ensure opentelemetryappender sees the right trace context
            try (Scope ignored = otelCtxAtEntry.makeCurrent()) {
                putMdcForOneLogLine(corrId, reqId);

                log.info("{} {} {} {}ms userId={} workspaceId={} ip={} ua=\"{}\" reqId={} corrId={}", method, path,
                        status, durationMs, nullToDash(userId), nullToDash(workspaceId), nullToDash(ip), nullToDash(ua),
                        nullToDash(reqId), nullToDash(corrId));
            } finally {
                clearMdcForOneLogLine();
            }
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // first ip
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isBlank()) {
            return xReal.trim();
        }
        return Optional.ofNullable(request.getRemoteAddr())
                .or(() -> Optional.ofNullable(request.getRemoteHost()))
                .orElseGet(() -> {
                    try {
                        return InetAddress.getLocalHost().getHostAddress();
                    } catch (Exception e) {
                        return "unknown";
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

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? null : value;
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
