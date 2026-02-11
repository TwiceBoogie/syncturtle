package com.syncturtle.platform.services.user.controllers.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.enums.AuthFlow;
import com.syncturtle.platform.services.user.application.query.AuthenticationQueryHandler;
import com.syncturtle.platform.services.user.dto.request.EmailCheckRequest;
import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.platform.services.user.utils.AuthFormValidator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationQueryHandler query;
    private final AuthFormValidator validator;

    @PostMapping("/email-check")
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody EmailCheckRequest request,
            BindingResult bindingResult) {
        validator.throwIfInvalid(AuthFlow.EMAIL_CHECK, bindingResult, request);

        return ResponseEntity.ok(query.emailCheck(request.getEmail()));
    }

}
