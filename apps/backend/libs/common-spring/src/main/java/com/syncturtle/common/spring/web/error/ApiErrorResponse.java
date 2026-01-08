package com.syncturtle.common.spring.web.error;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiErrorResponse {
    private final boolean ok;
    private final String error;
    private final String message;

    private final String traceId;
    private final String requestId;
    private final String correlationId;

    private final String path;
    private final Instant timestamp;

    private final List<FieldViolation> fields;

    public ApiErrorResponse(
            String error,
            String message,
            String traceId,
            String requestId,
            String correlationId,
            String path,
            Instant timestamp,
            List<FieldViolation> fields) {
        this.ok = false;
        this.error = error;
        this.message = message;
        this.traceId = traceId;
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.path = path;
        this.timestamp = timestamp;
        this.fields = fields;
    }

    public static ApiErrorResponse of(
            String error,
            String message,
            String traceId,
            String requestId,
            String correlationId,
            String path,
            List<FieldViolation> fields) {
        return new ApiErrorResponse(error, message, traceId, requestId, correlationId, path, Instant.now(), fields);
    }
}
