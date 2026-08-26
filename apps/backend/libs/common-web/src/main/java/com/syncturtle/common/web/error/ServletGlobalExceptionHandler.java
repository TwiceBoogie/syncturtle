package com.syncturtle.common.web.error;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.syncturtle.common.contracts.api.error.ApiErrorCode;
import com.syncturtle.common.contracts.api.error.ApiErrorResponse;
import com.syncturtle.common.contracts.api.error.FieldViolation;
import com.syncturtle.common.core.exception.SyncturtleException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class ServletGlobalExceptionHandler {

    private final ApiErrorResponseFactory responseFactory;

    public ServletGlobalExceptionHandler(ApiErrorResponseFactory responseFactory) {
        Assert.notNull(responseFactory, "api error response factory is required");

        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(SyncturtleException.class)
    public ResponseEntity<ApiErrorResponse> handleSyncturtleException(
            SyncturtleException exception,
            HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(exception.getHttpStatusCode());

        logAtExpectedLevel(status, exception);

        ApiErrorResponse body = responseFactory.fromSyncturtleException(
                exception,
                status,
                request);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> toFieldViolation(fieldError))
                .toList();

        ApiErrorResponse body = responseFactory.fromValidationErrors(fields, request);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldViolation> fields = exception.getConstraintViolations()
                .stream()
                .map(violation -> toFieldViolation(violation))
                .toList();

        ApiErrorResponse body = responseFactory.fromValidationErrors(fields, request);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        FieldViolation field = FieldViolation.builder()
                .field(exception.getParameterName())
                .errorMessage("REQUIRED")
                .message("Required parameter is missing.")
                .build();

        ApiErrorResponse body = responseFactory.fromValidationErrors(List.of(field), request);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        ApiErrorResponse body = responseFactory.fromStatus(
                ApiErrorCode.METHOD_NOT_ALLOWED,
                request);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        ApiErrorResponse body = responseFactory.fromStatus(
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                request);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    // @ExceptionHandler({ IllegalArgumentException.class,
    // IllegalStateException.class, NullPointerException.class })
    // public ResponseEntity<ApiErrorResponse>
    // handleDeveloperInvariantFailure(RuntimeException exception,
    // HttpServletRequest request) {
    // log.error("Developer invariant failed", exception);

    // ApiErrorResponse body = responseFactory.fromUnhandledException(exception,
    // request);

    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    // }

    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<ApiErrorResponse> handleUnhandledException(
    // Exception exception,
    // HttpServletRequest request) {
    // log.error("Unhandled exception", exception);

    // ApiErrorResponse body = responseFactory.fromUnhandledException(exception,
    // request);

    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    // }

    private static FieldViolation toFieldViolation(FieldError fieldError) {
        String errorMessage = fieldError.getCode() == null
                ? "INVALID_FIELD"
                : fieldError.getCode();

        String message = fieldError.getDefaultMessage() == null
                ? "Invalid field value."
                : fieldError.getDefaultMessage();

        return FieldViolation.builder()
                .field(fieldError.getField())
                .errorMessage(errorMessage)
                .message(message)
                .build();
    }

    private static FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null
                ? null
                : violation.getPropertyPath().toString();

        String message = violation.getMessage() == null
                ? "Invalid field value"
                : violation.getMessage();

        return FieldViolation.builder()
                .field(field)
                .errorMessage("INVALID_FIELD")
                .message(message)
                .build();
    }

    private static void logAtExpectedLevel(HttpStatusCode status, Exception exception) {
        if (status.is5xxServerError()) {
            log.error("Server error handled by global exception handler", exception);
            return;
        }

        if (status.value() == 401 || status.value() == 403) {
            log.debug("Auth/security exception handled: {}", exception.getMessage());
            return;
        }

        log.debug("Client exception handled: {}", exception.getMessage());
    }

}
