package com.syncturtle.services.instance.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceWorkspaceSlugCheckResponse {

    boolean available;
    String message;

    public static InstanceWorkspaceSlugCheckResponse available() {
        return InstanceWorkspaceSlugCheckResponse.builder()
                .available(true)
                .message(null)
                .build();
    }

    public static InstanceWorkspaceSlugCheckResponse unavailable(String message) {
        return InstanceWorkspaceSlugCheckResponse.builder()
                .available(false)
                .message(message)
                .build();
    }

}
