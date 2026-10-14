package com.syncturtle.services.file.policy;

import java.time.Duration;

public final class FileLifecyclePolicy {

    public static final Duration UPLOAD_IDEMPOTENCY_RETENTION = Duration.ofHours(24);
    public static final Duration UNLINKED_ASSET_RETENTION = Duration.ofDays(7);

    private FileLifecyclePolicy() {
    }

}
