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
    String slug;
    int role;
    long totalMembers;
    String organizationSize;
    String logoUrl;
    UUID logoAssetId;
    UserResponse owner;
    UUID createdById;
    UUID updatedById;
    Instant createdAt;
    Instant updatedAt;
}
