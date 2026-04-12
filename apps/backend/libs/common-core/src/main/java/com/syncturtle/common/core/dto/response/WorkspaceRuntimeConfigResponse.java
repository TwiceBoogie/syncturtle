package com.syncturtle.common.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRuntimeConfigResponse {
    private boolean workspaceCreationDisabled;
    private long version;
}
