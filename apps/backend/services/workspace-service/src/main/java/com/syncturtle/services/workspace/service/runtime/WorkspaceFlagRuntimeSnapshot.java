package com.syncturtle.services.workspace.service.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFlagRuntimeSnapshot {
    private boolean workspaceCreationDisabled;
    private Long version;
}
