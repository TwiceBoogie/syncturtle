package com.syncturtle.services.user.messaging.kafka.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.services.user.model.param.WorkspaceMemberInviteLiteReplicaParam;

@Component
public class WorkspaceMemberInviteEventMapper {

    public WorkspaceMemberInviteLiteReplicaParam toParam(WorkspaceMemberInviteEvent event) {
        Assert.notNull(event, "workspace member invite event is required");

        return WorkspaceMemberInviteLiteReplicaParam.builder()
                .id(event.getId())
                .workspaceId(event.getWorkspaceId())
                .email(event.getEmail())
                .accepted(event.isAccepted())
                .role(event.getRole())
                .respondedAt(event.getRespondedAt())
                .createdById(event.getCreatedById())
                .updatedById(event.getUpdatedById())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .deletedAt(event.getDeletedAt())
                .sourceVersion(event.getVersion())
                .build();
    }

}
