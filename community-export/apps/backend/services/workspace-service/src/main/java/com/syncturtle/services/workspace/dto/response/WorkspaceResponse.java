package com.syncturtle.services.workspace.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.web.pagination.CursorIdentifiable;

import lombok.Data;

@Data
public final class WorkspaceResponse implements CursorIdentifiable {
    private UUID id;
    private String name;
    private String logo;
    private UUID logoAssetId;
    private String slug;
    private String organizationSize;
    private UserResponse owner;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdById;
    private UUID updatedById;
}
