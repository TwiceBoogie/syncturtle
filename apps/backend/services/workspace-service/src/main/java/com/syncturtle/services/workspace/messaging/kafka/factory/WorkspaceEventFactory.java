package com.syncturtle.services.workspace.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceEventFactory {

    private final Clock clock;

    public WorkspaceEvent created(Workspace workspace) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_CREATED);
    }

    public WorkspaceEvent updated(Workspace workspace) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_UPDATED);
    }

    public WorkspaceEvent softDelete(Workspace workspace) {
        return from(workspace, WorkspaceEvent.Type.WORKSPACE_SOFT_DELETE);
    }

    public WorkspaceMemberEvent memberCreated(WorkspaceMember member, UUID workspaceId) {
        return fromMember(member, workspaceId, WorkspaceMemberEvent.Type.WORKSPACE_MEMBER_CREATED);
    }

    private WorkspaceEvent from(
            Workspace workspace,
            WorkspaceEvent.Type type) {
        Assert.notNull(workspace, "workspace is required");
        Assert.notNull(type, "workspace event type is required");

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
                .createdById(workspace.getCreatedById())
                .updatedById(workspace.getUpdatedById())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .deletedAt(workspace.getDeletedAt())
                .version(workspace.getVersion())
                .build();
    }

    private WorkspaceMemberEvent fromMember(
            WorkspaceMember member,
            UUID workspaceId,
            WorkspaceMemberEvent.Type type) {
        Assert.notNull(member, "workspace member is required");
        Assert.notNull(type, "workspace event type is required");

        return WorkspaceMemberEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .type(type)
                .id(member.getId())
                .workspaceId(workspaceId)
                .memberId(member.getMemberId())
                .role(member.getRole().getCode())
                .active(member.isActive())
                .createdById(member.getCreatedById())
                .updatedById(member.getUpdatedById())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .deletedAt(member.getDeletedAt())
                .version(member.getVersion())
                .build();
    }

}
