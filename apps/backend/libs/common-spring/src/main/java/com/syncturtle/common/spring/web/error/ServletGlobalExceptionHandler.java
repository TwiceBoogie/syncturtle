package com.syncturtle.common.spring.web.error;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.core.exceptions.SyncturtleException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServletGlobalExceptionHandler {

    private final ApiErrorResponseFactory responseFactory;

    public ServletGlobalExceptionHandler(ApiErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(
            AuthException exception,
            HttpServletRequest request) {
        HttpStatus status = resolveAuthStatus(exception.getAuthErrorCode());

        logAtExpectedLevel(status, exception);

        ApiErrorResponse body = responseFactory.fromSyncturtleException(
                exception,
                status,
                request,
                publicAuthMessage(exception.getAuthErrorCode()));

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
                .map(this::toFieldViolation)
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
        log.error("Unhandled servlet exception", exception);

        ApiErrorResponse body = responseFactory.fromUnhandledException(exception, request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private FieldViolation toFieldViolation(FieldError fieldError) {
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

    private static HttpStatus resolveAuthStatus(AuthErrorCode errorCode) {
        return switch (errorCode) {
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;

            case USER_DOES_NOT_EXIST,
                    AUTHENTICATION_FAILED,
                    AUTHENTICATION_FAILED_SIGN_IN,
                    AUTHENTICATION_FAILED_SIGN_UP,
                    ADMIN_AUTHENTICATION_FAILED,
                    INVALID_MAGIC_CODE_SIGN_IN,
                    INVALID_MAGIC_CODE_SIGN_UP,
                    INVALID_MAGIC_CODE_DEVICE_VERIFICATION,
                    EXPIRED_MAGIC_CODE_SIGN_IN,
                    EXPIRED_MAGIC_CODE_SIGN_UP,
                    EXPIRED_MAGIC_CODE_DEVICE,
                    INVALID_PASSWORD_TOKEN,
                    EXPIRED_PASSWORD_TOKEN ->
                HttpStatus.UNAUTHORIZED;

            case SIGNUP_DISABLED,
                    MAGIC_LINK_LOGIN_DISABLED,
                    PASSWORD_LOGIN_DISABLED,
                    EMAIL_PASSWORD_AUTHENTICATION_DISABLED,
                    USER_ACCOUNT_DEACTIVATED,
                    ADMIN_USER_DEACTIVATED ->
                HttpStatus.FORBIDDEN;

            case INSTANCE_NOT_CONFIGURED,
                    INVALID_EMAIL,
                    EMAIL_REQUIRED,
                    GENERIC_INPUT_ERROR,
                    INVALID_CSRF_TOKEN,
                    INVALID_PASSWORD,
                    SMTP_NOT_CONFIGURED,
                    INVALID_PASSWORD_CONFIRM,
                    USER_ALREADY_EXIST,
                    REQUIRED_EMAIL_PASSWORD_SIGN_UP,
                    INVALID_EMAIL_SIGN_UP,
                    INVALID_EMAIL_MAGIC_SIGN_UP,
                    MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED,
                    REQUIRED_EMAIL_PASSWORD_SIGN_IN,
                    INVALID_EMAIL_SIGN_IN,
                    INVALID_EMAIL_MAGIC_SIGN_IN,
                    MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED,
                    OAUTH_NOT_CONFIGURED,
                    GOOGLE_NOT_CONFIGURED,
                    GITHUB_NOT_CONFIGURED,
                    GITLAB_NOT_CONFIGURED,
                    GOOGLE_OAUTH_PROVIDER_ERROR,
                    GITHUB_OAUTH_PROVIDER_ERROR,
                    GITLAB_OAUTH_PROVIDER_ERROR,
                    INCORRECT_OLD_PASSWORD,
                    MISSING_PASSWORD,
                    INVALID_NEW_PASSWORD,
                    PASSWORD_ALREADY_SET,
                    ADMIN_ALREADY_EXIST,
                    REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME,
                    INVALID_ADMIN_EMAIL,
                    INVALID_ADMIN_PASSWORD,
                    REQUIRED_ADMIN_EMAIL_PASSWORD,
                    ADMIN_USER_ALREADY_EXIST,
                    ADMIN_USER_DOES_NOT_EXIST ->
                HttpStatus.BAD_REQUEST;

            case DEVICE_NOT_RECOGNIZED,
                    DEVICE_CODE_ATTEMPT_EXHAUSTED_VERIFICATION,
                    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN,
                    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_UP ->
                HttpStatus.BAD_REQUEST;
        };
    }

    private static String publicAuthMessage(AuthErrorCode errorCode) {
        return switch (errorCode) {
            case INSTANCE_NOT_CONFIGURED -> "Instance is not configured.";
            case INVALID_EMAIL,
                    INVALID_EMAIL_SIGN_IN,
                    INVALID_EMAIL_SIGN_UP,
                    INVALID_ADMIN_EMAIL,
                    INVALID_EMAIL_MAGIC_SIGN_IN,
                    INVALID_EMAIL_MAGIC_SIGN_UP ->
                "Enter a valid email address.";

            case EMAIL_REQUIRED -> "Email is required.";
            case INVALID_PASSWORD,
                    INVALID_ADMIN_PASSWORD,
                    INVALID_NEW_PASSWORD ->
                "Enter a valid password.";

            case INVALID_PASSWORD_CONFIRM -> "Passwords do not match.";
            case ADMIN_ALREADY_EXIST -> "An instance admin already exists.";
            case ADMIN_USER_ALREADY_EXIST,
                    USER_ALREADY_EXIST ->
                "A user with this email already exists.";

            case SIGNUP_DISABLED -> "Sign up is disabled.";
            case MAGIC_LINK_LOGIN_DISABLED -> "Magic link login is disabled.";
            case PASSWORD_LOGIN_DISABLED -> "Password login is disabled.";
            case USER_ACCOUNT_DEACTIVATED,
                    ADMIN_USER_DEACTIVATED ->
                "This account is deactivated.";

            case RATE_LIMIT_EXCEEDED -> "Too many attempts. Try again later.";

            default -> "Authentication failed.";
        };
    }

    private static HttpStatus resolveSyncturtleStatus(SyncturtleException exception) {
        if (exception instanceof HttpStatusAwareException statusAware) {
            return statusAware.getStatus();
        }

        return HttpStatus.BAD_REQUEST;
    }

    private static String resolveSyncturtlePublicMessage(
            SyncturtleException exception,
            HttpStatus status) {
        if (exception instanceof PublicMessageAwareException messageAware) {
            return messageAware.getPublicMessage();
        }

        if (status.is5xxServerError()) {
            return "Something went wrong.";
        }

        return exception.getErrorMessage();
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