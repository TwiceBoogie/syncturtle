package com.syncturtle.services.user.dto.response;

import lombok.Getter;

@Getter
public final class UserSessionRevocationResponse {

    private final boolean currentSessionRevoked;

    public UserSessionRevocationResponse(boolean currentSessionRevoked) {
        this.currentSessionRevoked = currentSessionRevoked;
    }

}
