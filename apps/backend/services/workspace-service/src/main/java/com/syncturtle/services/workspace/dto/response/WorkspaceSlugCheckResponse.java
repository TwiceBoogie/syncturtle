package com.syncturtle.services.workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class WorkspaceSlugCheckResponse {
    private boolean available;
    private String reason;
}
