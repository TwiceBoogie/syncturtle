package com.syncturtle.services.user.service.collaborator.authentication.oauth.gitlab;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class GitLabOAuthClientCredentials {

    private final String clientId;
    private final String clientSecret;
    private final String host;

    @Builder
    private GitLabOAuthClientCredentials(String clientId, String clientSecret, String host) {
        Assert.hasText(clientId, "gitlab client id is required");
        Assert.hasText(clientSecret, "gitlab client secret is required");
        Assert.hasText(host, "gitlab host is required");

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.host = host;
    }

}
