package com.syncturtle.common.cache.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class CachedResponsePayload {
    private int status;
    private String body;
    private String bodyType;
}
