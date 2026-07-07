package com.syncturtle.services.user.service.authentication.oauth.github;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Value;

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
