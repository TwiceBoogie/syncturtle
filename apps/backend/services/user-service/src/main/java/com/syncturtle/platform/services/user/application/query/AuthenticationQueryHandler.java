package com.syncturtle.platform.services.user.application.query;

import org.springframework.stereotype.Component;

import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.platform.services.user.services.AuthenticationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationQueryHandler {

    private final AuthenticationService authenticationService;

    public EmailCheckResponse emailCheck(String email) {
        return authenticationService.emailCheck(email);
    }

}
