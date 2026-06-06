package com.syncturtle.common.web.error.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import com.syncturtle.common.contracts.api.error.ApiErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public class RemoteServiceException extends SyncturtleServiceException {

    private RemoteServiceException(ApiErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
    }

    public static RemoteServiceException failed(
            String serviceName,
            int remoteStatus,
            Integer remoteCode,
            String remoteKey,
            String remoteTraceId,
            String remoteRequestId,
            String remoteCorrelationId,
            Throwable cause) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("remote_service", serviceName);
        payload.put("remote_status", remoteStatus);

        putIfPresent(payload, "remote_code", remoteCode);
        putIfHasText(payload, "remote_key", remoteKey);
        putIfHasText(payload, "remote_trace_id", remoteTraceId);
        putIfHasText(payload, "remote_request_id", remoteRequestId);
        putIfHasText(payload, "remote_correlation_id", remoteCorrelationId);

        return new RemoteServiceException(resolveCode(remoteStatus), payload, cause);
    }

    public static RemoteServiceException unavailable(
            String serviceName,
            Throwable cause) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("remote_service", serviceName);
        payload.put("remote_status", 503);

        return new RemoteServiceException(ApiErrorCode.REMOTE_SERVICE_UNAVAILABLE, payload, cause);
    }

    public static RemoteServiceException timeout(
            String serviceName,
            Throwable cause) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("remote_service", serviceName);
        payload.put("remote_status", 504);

        return new RemoteServiceException(ApiErrorCode.REMOTE_SERVICE_TIMEOUT, payload, cause);
    }

    private static ApiErrorCode resolveCode(int remoteStatus) {
        if (remoteStatus == 504) {
            return ApiErrorCode.REMOTE_SERVICE_TIMEOUT;
        }

        if (remoteStatus == 503) {
            return ApiErrorCode.REMOTE_SERVICE_UNAVAILABLE;
        }

        return ApiErrorCode.REMOTE_SERVICE_FAILED;
    }

    private static void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static void putIfHasText(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}