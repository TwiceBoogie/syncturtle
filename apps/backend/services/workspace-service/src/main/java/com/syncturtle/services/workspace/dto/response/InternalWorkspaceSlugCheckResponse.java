package com.syncturtle.services.workspace.dto.response;

import lombok.Value;

@Value
public class InternalWorkspaceSlugCheckResponse {
    boolean available;
    String message;

    public static InternalWorkspaceSlugCheckResponse unavailable(String message) {
        return new InternalWorkspaceSlugCheckResponse(false, message);
    }

    public static InternalWorkspaceSlugCheckResponse available() {
        return new InternalWorkspaceSlugCheckResponse(true, null);
    }
}
