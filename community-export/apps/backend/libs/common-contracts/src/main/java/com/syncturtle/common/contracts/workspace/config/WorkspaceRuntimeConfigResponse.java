package com.syncturtle.common.contracts.workspace.config;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class WorkspaceRuntimeConfigResponse {
    boolean workspaceCreationDisabled;
    Long version;
}
