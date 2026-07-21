package com.syncturtle.services.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserMeResponse {
    UUID id;
    String username;
    String mobileNumber;
    String email;
    String displayName;
    String firstName;
    String lastName;
    String avatarUrl;
    String coverImageUrl;
    UUID avatarAssetId;
    UUID coverImageAssetId;
    @JsonProperty("isActive")
    boolean active;
    @JsonProperty("isEmailVerified")
    boolean emailVerified;
    @JsonProperty("isPasswordAutoset")
    boolean passwordAutoset;
    @JsonProperty("isTourCompleted")
    boolean tourCompleted;
    String userTimezone;
    @JsonProperty("isBot")
    boolean bot;
    String lastLoginMedium;
    Instant createdAt;
    UUID lastWorkspaceId;
}
