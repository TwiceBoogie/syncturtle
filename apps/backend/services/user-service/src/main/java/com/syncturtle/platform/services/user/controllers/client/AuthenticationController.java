package com.syncturtle.platform.services.user.controllers.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.constants.EndpointConstants;
import com.syncturtle.common.core.constants.GatewayHeaderNames;
import com.syncturtle.common.core.enums.AuthFlow;
import com.syncturtle.platform.services.user.application.query.AuthenticationQueryHandler;
import com.syncturtle.platform.services.user.dto.request.EmailCheckRequest;
import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.platform.services.user.utils.AuthFormValidator;

import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointConstants.AUTH)
public class AuthenticationController {

    private final AuthenticationQueryHandler query;
    private final AuthFormValidator validator;

    @PostMapping(EndpointConstants.EMAIL__CHECK)
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody EmailCheckRequest request,
            BindingResult bindingResult) {
        validator.throwIfInvalid(AuthFlow.EMAIL_CHECK, bindingResult, request);

        return ResponseEntity.ok(query.emailCheck(request.getEmail()));
    }

    @PostMapping(EndpointConstants.SIGN__OUT)
    public ResponseEntity<Void> signOut(
            @RequestHeader(GatewayHeaderNames.HDR_INTERNAL_LOGOUT_CONTEXT) String logoutContext) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(query.signout(logoutContext)))
                .build();
    }

}
