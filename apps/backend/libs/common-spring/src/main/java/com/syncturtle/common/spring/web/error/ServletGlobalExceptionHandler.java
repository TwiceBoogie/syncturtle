package com.syncturtle.common.spring.web.error;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.ServletHostUrlBuilder;
import com.syncturtle.common.web.dto.response.AuthExceptionResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
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

        String redirectUrl = hostResolver.deriveOriginFromRequeset(request);

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }

}
