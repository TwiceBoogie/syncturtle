package com.syncturtle.services.user.client.oauth.google.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GoogleTokenResponse {
    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("expires_in")
    Long expiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("id_token")
    String idToken;
    String scope;
    @JsonProperty("token_type")
    String tokenType;
}
