package com.syncturtle.services.user.service.authentication.oauth.gitlab;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Value;

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
