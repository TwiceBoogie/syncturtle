package com.syncturtle.services.user.client.oauth.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHubTokenResponse {
    @JsonProperty("access_token")
    String accessToken;
    String scope;
    @JsonProperty("token_type")
    String tokenType;
    @JsonProperty("expires_in")
    Long expiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("id_token")
    String idToken;
}
