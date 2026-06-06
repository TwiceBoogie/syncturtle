package com.syncturtle.common.observability.interceptor;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.core.header.GatewayHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "access")
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START_NANOS = AccessLogInterceptor.class.getName() + ".startNanos";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception) {
        long start = (request.getAttribute(ATTR_START_NANOS) instanceof Long l)
                ? l
                : System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        int status = response.getStatus();
        // if something bad happen and status not set, log it as 500
        if (exception != null && status < 400) {
            status = 500;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        String ua = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_UA),
                nullToDash(request.getHeader("User-Agent")));
        String ip = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_IP),
                resolveClientIp(request));

        String userId = header(request, GatewayHeaders.HDR_AUTH_USER_ID);
        String workspaceId = header(request, GatewayHeaders.HDR_AUTH_WORKSPACE_ID);

        String corrId = firstNonBlank(
                response.getHeader(GatewayHeaders.HDR_CORRELATION_ID),
                request.getHeader(GatewayHeaders.HDR_CORRELATION_ID));
        String reqId = firstNonBlank(
                response.getHeader(GatewayHeaders.HDR_REQUEST_ID),
                request.getHeader(GatewayHeaders.HDR_REQUEST_ID));

        try {
            putMdcForOneLogLine(corrId, reqId);

            log.info("{} {} {} {}ms userId={} workspaceId={} ip={} ua=\"{}\" reqId={} corrId={}",
                    method,
                    path,
                    status,
                    durationMs,
                    nullToDash(userId),
                    nullToDash(workspaceId),
                    nullToDash(ip),
                    ua,
                    nullToDash(reqId),
                    nullToDash(corrId));
        } finally {
            clearMdcForOneLogLine();
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
        if (corrId != null && !corrId.isBlank()) {
            MDC.put("X-Correlation-Id", corrId);
        }
        if (reqId != null && !reqId.isBlank()) {
            MDC.put("X-Request-Id", reqId);
        }
    }

    private static void clearMdcForOneLogLine() {
        MDC.remove(GatewayHeaders.HDR_CORRELATION_ID);
        MDC.remove(GatewayHeaders.HDR_REQUEST_ID);
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
