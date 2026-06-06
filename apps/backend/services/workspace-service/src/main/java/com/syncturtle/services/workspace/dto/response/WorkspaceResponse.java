package com.syncturtle.services.workspace.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.web.pagination.CursorIdentifiable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkspaceResponse implements CursorIdentifiable {
    UUID id;
    String name;
    String logo;
    UUID logoAssetId;
    String slug;
    String organizationSize;
    UserResponse owner;
    Instant createdAt;
    Instant updatedAt;
    UUID createdById;
    UUID updatedById;
}
