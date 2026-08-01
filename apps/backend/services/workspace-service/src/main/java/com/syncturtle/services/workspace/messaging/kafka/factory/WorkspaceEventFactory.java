package com.syncturtle.services.workspace.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;

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

    public WorkspaceMemberInviteEvent memberInviteUpdated(WorkspaceMemberInvite invite) {
        return fromMemberInvite(invite, WorkspaceMemberInviteEvent.Type.WORKSPACE_MEMBER_INVITE_UPDATED);
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

    private WorkspaceMemberInviteEvent fromMemberInvite(WorkspaceMemberInvite invite,
            WorkspaceMemberInviteEvent.Type type) {
        Assert.notNull(invite, "workspace member invite is required");
        Assert.notNull(type, "workspace member invite event type is required");
        Assert.notNull(invite.getWorkspace(), "workspace is required");

        return WorkspaceMemberInviteEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .type(type)
                .id(invite.getId())
                .workspaceId(invite.getWorkspaceId())
                .email(invite.getEmail())
                .role(invite.getRole().getCode())
                .accepted(invite.isAccepted())
                .respondedAt(invite.getRespondedAt())
                .createdById(invite.getCreatedById())
                .updatedById(invite.getUpdatedById())
                .createdAt(invite.getCreatedAt())
                .updatedAt(invite.getUpdatedAt())
                .deletedAt(invite.getDeletedAt())
                .version(invite.getVersion())
                .build();
    }

}
