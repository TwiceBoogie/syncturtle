package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceWorkspaceOwnerResponse {
    UUID id;
    String username;
    String email;
    String displayName;
    String firstName;
    String lastName;
    Instant dateJoined;
    UUID avatarAssetId;
    UUID coverImageAssetId;
    boolean emailVerified;
    boolean passwordAutoset;
    String userTimezone;
    boolean bot;
}
