package com.syncturtle.services.user.application.query;

import org.springframework.stereotype.Component;

import com.syncturtle.services.user.dto.internal.RefreshExchangeResult;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.services.AuthenticationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationQueryHandler {

    private final AuthenticationService authenticationService;

    public EmailCheckResponse emailCheck(String email) {
        return authenticationService.emailCheck(email);
    }

    public IssueTokenResponse signin(SignInRequest request) {
        return authenticationService.signin(request);
    }

    public RefreshExchangeResult refreshSession(String refreshToken) {
        return authenticationService.refreshSession(refreshToken);
    }

    public String signout(String logoutContext, String sessionId) {
        return authenticationService.signOut(logoutContext, sessionId);
    }

}
