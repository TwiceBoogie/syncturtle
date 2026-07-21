package com.syncturtle.services.user.service.authentication.oauth.google;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.JsonNode;

@Value
@Builder
public final class GoogleOAuthUser {
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
