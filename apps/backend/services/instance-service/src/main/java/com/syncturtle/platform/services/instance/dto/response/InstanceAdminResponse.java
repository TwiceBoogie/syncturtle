package com.syncturtle.platform.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public final class InstanceAdminResponse {
    private UUID id;
    private int role;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdById;
    private UUID updatedById;
    @JsonProperty("instance")
    private UUID instanceId;
    @JsonProperty("user")
    private UUID userId;
    @JsonProperty("userDetail")
    private UserDetailsResponse user;

    @Data
    public static final class UserDetailsResponse {
        private UUID id;
        private String email;
        private String displayName;
        private String firstName;
        private String lastName;
        private UUID avatarAssetId;
        private boolean bot;
        private Instant dateJoined;
    }
}
