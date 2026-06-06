package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceAdminMeResponse {
    UUID id;
    UUID avatar;
    String avatarUrl;
    UUID coverImage;
    String coverImageUrl;
    Instant dateJoined;
    String displayName;
    String email;
    String firstName;
    String lastName;
    boolean bot;
    boolean emailVerified;
    String userTimezone;
    String username;
    boolean passwordAutoset;
    String lastLoginMedium;
}
