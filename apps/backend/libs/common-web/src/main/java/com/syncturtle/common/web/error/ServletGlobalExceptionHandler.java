package com.syncturtle.common.web.error;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.core.exceptions.SyncturtleException;
import com.syncturtle.common.web.error.exceptions.HttpStatusAwareException;
import com.syncturtle.common.web.error.exceptions.PublicMessageAwareException;
import com.syncturtle.common.web.error.mapper.AuthErrorPublicMessageMapper;
import com.syncturtle.common.web.error.mapper.AuthErrorStatusMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class ServletGlobalExceptionHandler {

    private final ApiErrorResponseFactory responseFactory;

    public ServletGlobalExceptionHandler(ApiErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(
            AuthException exception,
            HttpServletRequest request) {
        HttpStatus status = AuthErrorStatusMapper.toHttpStatus(exception.getAuthErrorCode());
        String publicMessage = AuthErrorPublicMessageMapper.toPublicMessage(exception.getAuthErrorCode());

        logAtExpectedLevel(status, exception);

        ApiErrorResponse body = responseFactory.fromSyncturtleException(
                exception,
                status,
                request,
                publicMessage);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(SyncturtleException.class)
    public ResponseEntity<ApiErrorResponse> handleSyncturtleException(
            SyncturtleException exception,
            HttpServletRequest request) {
        HttpStatus status = resolveSyncturtleStatus(exception);
        String publicMessage = resolveSyncturtlePublicMessage(exception, status);

        logAtExpectedLevel(status, exception);

        ApiErrorResponse body = responseFactory.fromSyncturtleException(
                exception,
                status,
                request,
                publicMessage);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(ServletGlobalExceptionHandler::toFieldViolation)
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
                "HTTP method is not allowed for this endpoint.",
                request);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        ApiErrorResponse body = responseFactory.fromStatus(
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported.",
                request);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(
            Exception exception,
            HttpServletRequest request) {
        ApiErrorResponse body = responseFactory.fromUnhandledException(exception, request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

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

    private static HttpStatus resolveSyncturtleStatus(SyncturtleException exception) {
        if (exception instanceof HttpStatusAwareException statusAware) {
            HttpStatus status = statusAware.getStatus();
            if (status != null) {
                return status;
            }
        }

        return HttpStatus.BAD_REQUEST;
    }

    private static String resolveSyncturtlePublicMessage(
            SyncturtleException exception,
            HttpStatus status) {
        if (exception instanceof PublicMessageAwareException messageAware) {
            String publicMessage = messageAware.getPublicMessage();
            if (StringUtils.hasText(publicMessage)) {
                return publicMessage;
            }
        }

        if (status.is5xxServerError()) {
            return "Something went wrong.";
        }

        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }

        if (exception.getErrorCode() != null) {
            return exception.getErrorMessage();
        }

        return "Request failed.";
    }

    private static void logAtExpectedLevel(HttpStatus status, Exception exception) {
        if (status.is5xxServerError()) {
            log.error("Server error handled by global exception handler", exception);
            return;
        }

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            log.debug("Auth/security exception handled: {}", exception.getMessage());
            return;
        }

        log.debug("Client exception handled: {}", exception.getMessage());
    }

}
