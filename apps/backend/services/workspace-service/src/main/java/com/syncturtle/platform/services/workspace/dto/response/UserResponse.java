package com.syncturtle.platform.services.workspace.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public final class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private Instant dateJoined;
    private UUID avatarAssetId;
    private UUID coverImageAssetId;
    private boolean active;
    private boolean emailVerified;
    private boolean passwordAutoset;
    private String timezone;
    private boolean bot;
}
