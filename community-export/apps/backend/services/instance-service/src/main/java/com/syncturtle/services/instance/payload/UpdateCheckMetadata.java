package com.syncturtle.services.instance.payload;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class UpdateCheckMetadata {
    private final String latestVersion;
    private final Instant lastCheckedAt;
}
