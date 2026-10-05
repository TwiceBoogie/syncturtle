package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserAdminLiteResponse {
    UUID id;
    String email;
    String firstName;
    String lastName;
    String displayName;
    String avatarUrl;
    Instant dateJoined;
}
