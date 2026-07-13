package com.syncturtle.services.user.messaging.kafka.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.services.user.model.param.WorkspaceMemberLiteReplicaParam;

@Component
public class WorkspaceMemberEventMapper {

    public WorkspaceMemberLiteReplicaParam toParam(WorkspaceMemberEvent event) {
        Assert.notNull(event, "workspace member event is required");

        return WorkspaceMemberLiteReplicaParam.builder()
                .id(event.getId())
                .workspaceId(event.getWorkspaceId())
                .memberId(event.getMemberId())
                .role(event.getRole())
                .active(event.isActive())
                .createdById(event.getCreatedById())
                .updatedById(event.getUpdatedById())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .deletedAt(event.getDeletedAt())
                .sourceVersion(event.getVersion())
                .build();
    }

}
