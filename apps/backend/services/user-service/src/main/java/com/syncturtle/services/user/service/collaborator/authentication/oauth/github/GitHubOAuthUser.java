package com.syncturtle.services.user.service.collaborator.authentication.oauth.github;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.JsonNode;

@Value
@Builder
public class GitHubOAuthUser {
    String providerAccountId;
    String email;
    String avatarUrl;
    String firstName;
    String lastName;
    String displayName;
    String accessToken;
    Instant accessTokenExpiredAt;
    String refreshToken;
    Instant refreshTokenExpiredAt;
    String idToken;
    JsonNode metadata;
}
