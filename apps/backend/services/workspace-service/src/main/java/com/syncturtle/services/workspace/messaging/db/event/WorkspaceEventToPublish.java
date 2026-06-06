package com.syncturtle.services.workspace.messaging.db.event;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;

import lombok.Getter;

/**
 * small app event wrapper
 */
@Getter
public final class WorkspaceEventToPublish {
    private final WorkspaceEvent event;

    public WorkspaceEventToPublish(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");
        this.event = event;
    }
}
