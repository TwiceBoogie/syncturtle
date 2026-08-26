package com.syncturtle.services.user.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.services.user.dto.request.ForgotPasswordRequest;
import com.syncturtle.services.user.dto.request.ResetPasswordFormRequest;
import com.syncturtle.services.user.dto.response.SimpleMessageResponse;
import com.syncturtle.services.user.service.PasswordResetService;
import com.syncturtle.services.user.service.collaborator.authentication.redirect.AuthenticationRedirector;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService service;
    private final AuthenticationRedirector authenticationRedirector;

    @PostMapping("/forgot-password")
    public ResponseEntity<SimpleMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL);
        }

        service.requestReset(request.getEmail());

        return ResponseEntity.ok(SimpleMessageResponse.builder()
                .message("Check your email to reset your password")
                .build());
    }

    @PostMapping("/reset-password/{uidb64}/{token}")
    public ResponseEntity<Void> resetPassword(@PathVariable String uidb64, @PathVariable String token,
            @Valid @ModelAttribute ResetPasswordFormRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            AuthException exception = AuthException.of(AuthErrorCode.INVALID_PASSWORD);

            return redirect(authenticationRedirector.passwordResetFailure(uidb64, token, exception));
        }

        try {
            service.resetPassword(uidb64, token, request.getPassword());

            return redirect(authenticationRedirector.passwordResetSuccess());
        } catch (AuthException exception) {
            return redirect(authenticationRedirector.passwordResetFailure(uidb64, token, exception));
        }
    }

    private static ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(location))
                .build();
    }

}
