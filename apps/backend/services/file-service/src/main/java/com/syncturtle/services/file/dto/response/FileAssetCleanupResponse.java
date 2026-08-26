package com.syncturtle.services.file.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileAssetCleanupResponse {
    int assetsClaimed;
    int objectsDeleted;
    int objectDeletionFailures;
    int idempotencyRecordsDeleted;
}
