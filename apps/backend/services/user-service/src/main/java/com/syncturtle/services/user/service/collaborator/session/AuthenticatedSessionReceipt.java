package com.syncturtle.services.user.service.collaborator.session;

import org.springframework.util.Assert;

import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;

import lombok.Builder;
import lombok.Value;

@Value
public class AuthenticatedSessionReceipt {

    IssuedAccessTokenReceipt accessToken;
    IssuedRefreshTokenReceipt refreshToken;

    @Builder
    private AuthenticatedSessionReceipt(
            IssuedAccessTokenReceipt accessToken,
            IssuedRefreshTokenReceipt refreshToken) {
        Assert.notNull(accessToken, "accessToken is required");
        Assert.notNull(refreshToken, "refreshToken is required");

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

}
