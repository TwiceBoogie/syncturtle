package com.syncturtle.services.user.service.authentication.oauth.gitlab;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.JsonNode;

@Value
@Builder
public class GitLabOAuthUser {
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
