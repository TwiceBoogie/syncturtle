package com.syncturtle.services.user.service.collaborator.authentication.oauth.github;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class GitHubOAuthClientCredentials {

    private final String clientId;
    private final String clientSecret;
    private final String organizationId;

    @Builder
    private GitHubOAuthClientCredentials(String clientId, String clientSecret, String organizationId) {
        Assert.hasText(clientId, "github clientId is required");
        Assert.hasText(clientSecret, "github client secret is required");

        this.clientId = clientId.trim();
        this.clientSecret = clientSecret.trim();
        this.organizationId = normalizeNullable(organizationId);
    }

    public boolean hasOrganizationId() {
        return organizationId != null;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
