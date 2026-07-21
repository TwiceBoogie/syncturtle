package com.syncturtle.common.observability.interceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.core.header.GatewayHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "access")
public final class AccessLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START_NANOS = AccessLogInterceptor.class.getName() + ".startNanos";
    private static final String MDC_CORRELATION_ID = GatewayHeaders.HDR_CORRELATION_ID;
    private static final String MDC_REQUEST_ID = GatewayHeaders.HDR_REQUEST_ID;
    private static final int MAX_LOG_VALUE_LENGTH = 1_024;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception) {
        long durationMillis = durationMillis(request);

        int status = resolveStatus(response, exception);

        String correlationId = firstNonBlank(
                response.getHeader(GatewayHeaders.HDR_CORRELATION_ID),
                request.getHeader(GatewayHeaders.HDR_CORRELATION_ID));

        String requestId = firstNonBlank(
                response.getHeader(GatewayHeaders.HDR_REQUEST_ID),
                request.getHeader(GatewayHeaders.HDR_REQUEST_ID));

        String userAgent = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_UA),
                request.getHeader("User-Agent"));

        /*
         * Downstream services should prefer the IP normalized by the trusted
         * API gateway. Falling back directly to remoteAddr avoids trusting a
         * client-controlled X-Forwarded-For header.
         */
        String clientIp = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_IP),
                request.getRemoteAddr());

        String userId = request.getHeader(GatewayHeaders.HDR_AUTH_USER_ID);

        String workspaceId = request.getHeader(GatewayHeaders.HDR_AUTH_WORKSPACE_ID);

        String exceptionType = exception == null ? null : exception.getClass().getName();

        Map<String, String> previousMdc = MDC.getCopyOfContextMap();

        try {
            putMdc(correlationId, requestId);

            log.info(
                    "method={} path={} status={} durationMs={} "
                            + "userId={} workspaceId={} ip={} "
                            + "ua=\"{}\" requestId={} "
                            + "correlationId={} exception={}",
                    logValue(request.getMethod()),
                    logValue(request.getRequestURI()),
                    status,
                    durationMillis,
                    logValue(userId),
                    logValue(workspaceId),
                    logValue(clientIp),
                    logValue(userAgent),
                    logValue(requestId),
                    logValue(correlationId),
                    logValue(exceptionType));
        } finally {
            restoreMdc(previousMdc);
        }
    }

    private static long durationMillis(HttpServletRequest request) {
        Object startAttribute = request.getAttribute(ATTR_START_NANOS);

        if (!(startAttribute instanceof Long startNanos)) {
            return 0L;
        }

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static int resolveStatus(HttpServletResponse response, Exception exception) {
        int status = response.getStatus();

        if (exception != null && status < 400) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        return status;
    }

    private static void putMdc(String correlationId, String requestId) {
        if (hasText(correlationId)) {
            MDC.put(MDC_CORRELATION_ID, correlationId);
        }

        if (hasText(requestId)) {
            MDC.put(MDC_REQUEST_ID, requestId);
        }
    }

    private static void restoreMdc(Map<String, String> previousMdc) {
        MDC.clear();

        if (previousMdc != null && !previousMdc.isEmpty()) {
            MDC.setContextMap(previousMdc);
        }
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