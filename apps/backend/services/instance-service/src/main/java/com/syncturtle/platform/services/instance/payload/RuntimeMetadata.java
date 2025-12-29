package com.syncturtle.platform.services.instance.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class RuntimeMetadata {
    private final String domain;
    private final String namespace;
    private final String vmHost;
}
