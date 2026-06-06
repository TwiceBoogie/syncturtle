package com.syncturtle.services.workspace.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.workspace.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceEventFactory {

    private final Clock clock;

    public WorkspaceEvent created(Workspace workspace, long totalMembers) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_CREATED, totalMembers);
    }

    public WorkspaceEvent updated(Workspace workspace, long totalMembers) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_UPDATED, totalMembers);
    }

    public WorkspaceEvent softDelete(Workspace workspace, long totalMembers) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_SOFT_DELETE, totalMembers);
    }

    private WorkspaceEvent from(
            Workspace workspace,
            WorkspaceEvent.Type type,
            long totalMembers) {
        Assert.notNull(workspace, "workspace is required");
        Assert.notNull(type, "workspace event type is required");
        Assert.isTrue(totalMembers >= 0, "totalMembers must be greater than or equal to 0");

        return WorkspaceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .type(type)
                .id(workspace.getId())
                .name(workspace.getName())
                .logoAssetId(workspace.getLogoAssetId())
                .slug(workspace.getSlug())
                .organizationSize(workspace.getOrganizationSize())
                .ownerId(workspace.getOwnerId())
                .timezone(workspace.getTimezone())
                .totalMembers(totalMembers)
                .createdById(workspace.getCreatedById())
                .updatedById(workspace.getUpdatedById())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .deletedAt(workspace.getDeletedAt())
                .version(workspace.getVersion())
                .build();
    }

}
