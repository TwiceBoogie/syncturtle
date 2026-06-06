package com.syncturtle.services.instance.messaging.kafka.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.instance.model.param.WorkspaceReplicaParam;

@Component
public final class WorkspaceEventMapper {

    public WorkspaceReplicaParam toParam(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        return WorkspaceReplicaParam.of(
                event.getId(),
                event.getName(),
                event.getLogo(),
                event.getLogoAssetId(),
                event.getSlug(),
                event.getOrganizationSize(),
                event.getOwnerId(),
                event.getTimezone(),
                event.getTotalMembers(),
                event.getCreatedById(),
                event.getUpdatedById(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getDeletedAt(),
                event.getVersion());
    }

}
