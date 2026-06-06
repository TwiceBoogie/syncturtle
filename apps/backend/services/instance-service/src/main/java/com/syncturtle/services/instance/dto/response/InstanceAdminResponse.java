package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.services.instance.type.InstanceAdminRole;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceAdminResponse {
    UUID id;
    UUID instance;
    UUID user;
    InstanceAdminRole role;
    Instant createdAt;
    Instant updatedAt;
    UserAdminLiteResponse userDetail;
    UUID createdBy;
    UUID updatedBy;
}
