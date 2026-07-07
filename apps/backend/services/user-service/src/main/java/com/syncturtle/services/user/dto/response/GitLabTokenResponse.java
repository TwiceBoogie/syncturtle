package com.syncturtle.services.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitLabTokenResponse {
    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("token_type")
    String tokenType;
    @JsonProperty("expires_in")
    Long expiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("created_at")
    Long createdAt;
    @JsonProperty("id_token")
    String idToken;
    String scope;
}
