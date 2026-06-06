package com.syncturtle.services.user.dto.response;

import com.syncturtle.services.user.type.AuthenticationFlowType;

import lombok.Getter;

@Getter
public final class EmailCheckResponse {
    private final boolean existingUser;
    private final AuthenticationFlowType authenticationFlow;

    private EmailCheckResponse(boolean existingUser, AuthenticationFlowType authenticationFlow) {
        this.existingUser = existingUser;
        this.authenticationFlow = authenticationFlow;
    }

    public static EmailCheckResponse forExistingUser(AuthenticationFlowType authenticationFlow) {
        return new EmailCheckResponse(true, authenticationFlow);
    }

    public static EmailCheckResponse forNewUser(AuthenticationFlowType authenticationFlow) {
        return new EmailCheckResponse(false, authenticationFlow);
    }
}
