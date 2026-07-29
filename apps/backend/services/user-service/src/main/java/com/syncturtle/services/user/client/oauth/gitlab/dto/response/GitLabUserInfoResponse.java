package com.syncturtle.services.user.client.oauth.gitlab.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitLabUserInfoResponse {
    Long id;
    String username;
    String name;
    String email;
    @JsonProperty("avatar_url")
    String avatarUrl;
}
