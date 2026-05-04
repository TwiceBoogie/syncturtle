package com.syncturtle.common.spring.web.error;

import java.util.Arrays;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.api.error.ApiErrorCode;
import com.syncturtle.common.contracts.api.error.ApiErrorResponse;
import com.syncturtle.common.contracts.api.error.DebugErrorDetails;
import com.syncturtle.common.contracts.api.error.FieldViolation;
import com.syncturtle.common.core.error.ErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleException;
import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.spring.properties.ErrorResponseProperties;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;

public class ApiErrorResponseFactory {

    private final ErrorResponseProperties properties;
    private final Environment environment;

    public ApiErrorResponseFactory(
            ErrorResponseProperties properties,
            Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public ApiErrorResponse fromSyncturtleException(
            SyncturtleException exception,
            HttpStatus status,
            HttpServletRequest request,
            String publicMessage) {
        ErrorCode errorCode = exception.getErrorCode();

        return baseBuilder(errorCode, request)
                .message(resolvePublicMessage(publicMessage, errorCode, status))
                .meta(exception.getPayload())
                .debug(debugDetails(exception))
                .build();
    }

    public ApiErrorResponse fromValidationErrors(
            List<FieldViolation> fields,
            HttpServletRequest request) {
        return baseBuilder(ApiErrorCode.VALIDATION_FAILED, request)
                .message("One or more fields are invalid.")
                .fields(fields)
                .build();
    }

    public ApiErrorResponse fromUnhandledException(
            Exception exception,
            HttpServletRequest request) {
        return baseBuilder(ApiErrorCode.INTERNAL_SERVER_ERROR, request)
                .message("Something went wrong.")
                .debug(debugDetails(exception))
                .build();
    }

    public ApiErrorResponse fromStatus(
            ApiErrorCode errorCode,
            String publicMessage,
            HttpServletRequest request) {
        return baseBuilder(errorCode, request)
                .message(publicMessage)
                .build();
    }

    private ApiErrorResponse.ApiErrorResponseBuilder baseBuilder(
            ErrorCode errorCode,
            HttpServletRequest request) {
        ApiErrorResponse.ApiErrorResponseBuilder builder = ApiErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .errorMessage(errorCode.getMessage());

        if (properties.isIncludeRequestIds()) {
            builder.traceId(resolveTraceId());
            builder.requestId(resolveRequestId(request));
            builder.correlationId(resolveCorrelationId(request));
        }

        if (properties.isIncludePath() && request != null) {
            builder.path(request.getRequestURI());
        }

        return builder;
    }

    private String resolvePublicMessage(String publicMessage, ErrorCode errorCode, HttpStatus status) {
        if (StringUtils.hasText(publicMessage)) {
            return publicMessage;
        }

        if (status.is5xxServerError()) {
            return "Something went wrong.";
        }

        return errorCode.getMessage();
    }

    private DebugErrorDetails debugDetails(Exception exception) {
        if (!shouldIncludeDebug()) {
            return null;
        }

        DebugErrorDetails.DebugErrorDetailsBuilder builder = DebugErrorDetails.builder()
                .exception(exception.getClass().getName())
                .exceptionMessage(exception.getMessage())
                .cause(exception.getCause() == null ? null : exception.getCause().getClass().getName());

        if (properties.isIncludeStackTrace()) {
            Arrays.stream(exception.getStackTrace())
                    .limit(Math.max(0, properties.getMaxStackTraceLines()))
                    .map(StackTraceElement::toString)
                    .forEach(builder::stackTraceLine);
        }

        return builder.build();
    }

    private boolean shouldIncludeDebug() {
        if (properties.isIncludeDebug()) {
            return true;
        }

        // fallback
        return environment.matchesProfiles("local | dev | development | test | setup");
    }

    private String resolveTraceId() {
        String fromMdc = firstNonBlank(
                MDC.get("traceId"),
                MDC.get("trace_id"));

        if (fromMdc != null) {
            return fromMdc;
        }

        Span span = Span.current();
        if (span != null && span.getSpanContext().isValid()) {
            return span.getSpanContext().getTraceId();
        }

        return null;
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return firstNonBlank(
                    MDC.get(GatewayHeaders.HDR_REQUEST_ID),
                    MDC.get("requestId"));
        }

        return firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_REQUEST_ID),
                MDC.get(GatewayHeaders.HDR_REQUEST_ID),
                MDC.get("requestId"));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        if (request == null) {
            return firstNonBlank(
                    MDC.get(GatewayHeaders.HDR_CORRELATION_ID),
                    MDC.get("correlationId"));
        }

        return firstNonBlank(
                request.getHeader(GatewayHeaders.HDR_CORRELATION_ID),
                MDC.get(GatewayHeaders.HDR_CORRELATION_ID),
                MDC.get("correlationId"));
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

}
