package com.syncturtle.common.spring.web.error;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.ServletHostUrlBuilder;
import com.syncturtle.common.web.dto.response.AuthExceptionResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ServletGlobalExceptionHandler {

    private final ServletHostUrlBuilder hostResolver;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handle(AuthenticationException exception, HttpServletRequest request) {
        boolean isFormPost = request.getContentType() != null
                && request.getContentType().contains("application/x-www-form-urlencoded");

        if (!isFormPost) {
            AuthErrorCode authErrorCode = exception.getErrorCode();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthExceptionResponse(authErrorCode.getCode(),
                    authErrorCode.getMessage(), exception.getPayload()));
        }

        String redirectUrl;
        if (exception.getErrorCode().toString().contains("ADMIN")) {
            redirectUrl = hostResolver.buildAdminRedirectUrlWithErrors(request, exception.getErrorMap());
        } else {
            redirectUrl = hostResolver.buildRedirectUrlWithErrors(request, exception.getErrorMap());
        }

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> fields = exception.getBindingResult().getFieldErrors().stream().map(this::toViolation)
                .collect(Collectors.toList());

        ApiErrorResponse body = ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", traceId(),
                requestId(request), correlationId(request), request.getRequestURI(), fields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private FieldViolation toViolation(FieldError fieldError) {
        return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String traceId() {
        return MDC.get("traceId");
    }

    private String requestId(HttpServletRequest request) {
        String fromMdc = MDC.get("requestId");
        if (fromMdc != null) {
            return fromMdc;
        }
        return request.getHeader("X-Request-Id");
    }

    private String correlationId(HttpServletRequest request) {
        String fromMdc = MDC.get("correlationId");
        if (fromMdc != null) {
            return fromMdc;
        }
        return request.getHeader("X-Correlation-Id");
    }

}
