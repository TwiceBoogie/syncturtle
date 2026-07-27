package com.syncturtle.services.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHubUserInfoResponse {
    Long id;
    String login;
    String name;
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("html_url")
    String htmlUrl;
}
