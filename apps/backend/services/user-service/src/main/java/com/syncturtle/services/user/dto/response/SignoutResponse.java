package com.syncturtle.services.user.dto.response;

import lombok.Getter;

@Getter
public final class SignOutResponse {
    private final String redirection;

    private SignOutResponse(String redirection) {
        this.redirection = redirection;
    }

    public static SignOutResponse redirect(String location) {
        return new SignOutResponse(location);
    }
}
