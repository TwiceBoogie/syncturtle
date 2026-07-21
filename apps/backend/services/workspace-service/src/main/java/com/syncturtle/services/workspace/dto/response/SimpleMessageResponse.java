package com.syncturtle.services.workspace.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleMessageResponse {
    String message;
}
