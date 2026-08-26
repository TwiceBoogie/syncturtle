package com.syncturtle.services.user.client.oauth.google.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GoogleUserInfoResponse {
    String id;
    String email;
    String picture;
    @JsonProperty("given_name")
    String givenName;
    @JsonProperty("family_name")
    String familyName;
}
