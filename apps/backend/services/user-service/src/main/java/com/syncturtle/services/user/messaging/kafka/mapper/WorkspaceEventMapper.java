package com.syncturtle.services.user.messaging.kafka.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.user.model.param.WorkspaceLiteReplicaParam;

@Component
public class WorkspaceEventMapper {

    public WorkspaceLiteReplicaParam toParam(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        return WorkspaceLiteReplicaParam.builder()
                .id(event.getId())
                .name(event.getName())
                .logoAssetId(event.getLogoAssetId())
                .slug(event.getSlug())
                .createdById(event.getCreatedById())
                .updatedById(event.getUpdatedById())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .deletedAt(event.getDeletedAt())
                .sourceVersion(event.getVersion())
                .build();
    }

}
