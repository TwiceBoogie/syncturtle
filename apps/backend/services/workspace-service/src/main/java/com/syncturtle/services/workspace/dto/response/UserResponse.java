package com.syncturtle.services.workspace.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {
    UUID id;
    String displayName;
    String email;
    String firstName;
    String lastName;
    Instant dateJoined;
    String avatarUrl;
    UUID avatarAssetId;
    boolean bot;
}
