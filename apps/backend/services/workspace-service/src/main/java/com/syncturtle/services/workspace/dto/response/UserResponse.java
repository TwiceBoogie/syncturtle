package com.syncturtle.services.workspace.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {
    UUID id;
    String username;
    String email;
    String displayName;
    String firstName;
    String lastName;
    Instant dateJoined;
    UUID avatarAssetId;
    UUID coverImageAssetId;
    boolean active;
    boolean emailVerified;
    boolean passwordAutoset;
    String timezone;
    boolean bot;
}
