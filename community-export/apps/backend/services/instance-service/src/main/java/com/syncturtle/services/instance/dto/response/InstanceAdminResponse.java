package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceAdminResponse {
    UUID id;
    UUID instance;
    UUID user;
    int role;
    Instant createdAt;
    Instant updatedAt;
    UserAdminLiteResponse userDetail;
    UUID createdBy;
    UUID updatedBy;
}
