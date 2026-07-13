package com.syncturtle.services.user.service.authentication.oauth.google;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class GoogleOAuthClientCredentials {

    private final String clientId;
    private final String clientSecret;

    @Builder
    private GoogleOAuthClientCredentials(String clientId, String clientSecret) {
        Assert.hasText(clientId, "google clientId is required");
        Assert.hasText(clientSecret, "google clientSecret is required");

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

}
