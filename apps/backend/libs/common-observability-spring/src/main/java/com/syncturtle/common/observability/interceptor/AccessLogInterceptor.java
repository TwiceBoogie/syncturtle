package com.syncturtle.common.observability.interceptor;

import java.util.concurrent.TimeUnit;

import org.springframework.web.servlet.HandlerInterceptor;

import com.syncturtle.common.core.header.GatewayHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "access")
public final class AccessLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START_NANOS = AccessLogInterceptor.class.getName() + ".startNanos";
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

        String userAgent = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_UA),
                request.getHeader("User-Agent"));

        String clientIp = firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CLIENT_IP),
                request.getRemoteAddr());

        String userId = request.getHeader(GatewayHeaders.HDR_AUTH_USER_ID);

        String workspaceId = request.getHeader(GatewayHeaders.HDR_AUTH_WORKSPACE_ID);

        String exceptionType = exception == null ? null : exception.getClass().getName();

        log.info(
                "method={} path={} status={} durationMs={} "
                        + "userId={} workspaceId={} ip={} "
                        + "ua=\"{}\" exception={}",
                logValue(request.getMethod()),
                logValue(request.getRequestURI()),
                status,
                durationMillis,
                logValue(userId),
                logValue(workspaceId),
                logValue(clientIp),
                logValue(userAgent),
                logValue(exceptionType));
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