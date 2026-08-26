package com.syncturtle.common.web.error;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;

import com.syncturtle.common.contracts.api.error.ApiErrorCode;
import com.syncturtle.common.contracts.api.error.ApiErrorResponse;
import com.syncturtle.common.contracts.api.error.DebugErrorDetails;
import com.syncturtle.common.contracts.api.error.FieldViolation;
import com.syncturtle.common.core.error.ErrorCode;
import com.syncturtle.common.core.exception.SyncturtleException;
import com.syncturtle.common.web.property.ErrorResponseProperties;

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
            HttpStatusCode status,
            HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();

        return baseBuilder(errorCode, request)
                .message(resolvePublicMessage(exception, status))
                .meta(exception.getPayload())
                .debug(debugDetails(exception))
                .build();
    }

    public ApiErrorResponse fromValidationErrors(
            List<FieldViolation> fields,
            HttpServletRequest request) {
        return baseBuilder(ApiErrorCode.VALIDATION_FAILED, request)
                .message(ApiErrorCode.VALIDATION_FAILED.getPublicMessage())
                .fields(fields)
                .build();
    }

    public ApiErrorResponse fromUnhandledException(
            Exception exception,
            HttpServletRequest request) {
        return baseBuilder(ApiErrorCode.INTERNAL_SERVER_ERROR, request)
                .message(ApiErrorCode.INTERNAL_SERVER_ERROR.getPublicMessage())
                .debug(debugDetails(exception))
                .build();
    }

    public ApiErrorResponse fromStatus(
            ApiErrorCode errorCode,
            HttpServletRequest request) {
        return baseBuilder(errorCode, request)
                .message(errorCode.getPublicMessage())
                .build();
    }

    private ApiErrorResponse.ApiErrorResponseBuilder baseBuilder(
            ErrorCode errorCode,
            HttpServletRequest request) {
        ApiErrorResponse.ApiErrorResponseBuilder builder = ApiErrorResponse.builder()
                .code(errorCode.getCode())
                .key(errorCode.getKey());

        if (properties.isIncludeTraceId()) {
            builder.traceId(TraceIds.resolve());
        }

        if (properties.isIncludePath() && request != null) {
            builder.path(request.getRequestURI());
        }

        return builder;
    }

    private String resolvePublicMessage(SyncturtleException exception, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return exception.getPublicMessage() == null || exception.getPublicMessage().isBlank()
                    ? "Something went wrong."
                    : exception.getPublicMessage();
        }

        return exception.getPublicMessage();
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
        return environment.matchesProfiles("local", "dev", "development", "test", "setup");
    }

}
