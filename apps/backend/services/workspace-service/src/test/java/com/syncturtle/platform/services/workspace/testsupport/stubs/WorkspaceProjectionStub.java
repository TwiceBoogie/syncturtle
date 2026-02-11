package com.syncturtle.platform.services.workspace.testsupport.stubs;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.platform.services.workspace.repositories.projections.UserProjection;
import com.syncturtle.platform.services.workspace.repositories.projections.WorkspaceProjection;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class WorkspaceProjectionStub implements WorkspaceProjection {

    private static final Instant DEFAULT_TIME = Instant.parse("2026-02-10T00:00:00Z");

    @Builder.Default
    private final UUID id = UUID.randomUUID();
    @Builder.Default
    private final String name = "Marvel";
    @Builder.Default
    private final String logo = null;
    @Builder.Default
    private final UUID logoAssetId = null;
    @Builder.Default
    private final String slug = "marvel";
    @Builder.Default
    private final String organizationSize = "just myself";
    @Builder.Default
    private final UserProjection owner = null;
    @Builder.Default
    private final Instant createdAt = DEFAULT_TIME;
    @Builder.Default
    private final Instant updatedAt = DEFAULT_TIME;
    @Builder.Default
    private final UUID createdById = UUID.randomUUID();
    @Builder.Default
    private final UUID updatedById = UUID.randomUUID();

}
