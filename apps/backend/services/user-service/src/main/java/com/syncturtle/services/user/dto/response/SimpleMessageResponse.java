package com.syncturtle.services.user.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleMessageResponse {
    String message;

    public static SimpleMessageResponse updated() {
        return SimpleMessageResponse.builder()
                .message("Updated successfully")
                .build();
    }
}
