package com.syncturtle.services.workspace.dto.response;

import lombok.Value;

@Value
public class WorkspaceSlugCheckResponse {
    boolean available;
    String message;

    public static WorkspaceSlugCheckResponse unavailable(String message) {
        return new WorkspaceSlugCheckResponse(false, message);
    }

    public static WorkspaceSlugCheckResponse available() {
        return new WorkspaceSlugCheckResponse(true, null);
    }
}
