package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public final class UserMeResponse {
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
    private String userTimezone;
}
