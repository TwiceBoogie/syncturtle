package com.syncturtle.services.file.service;

import com.syncturtle.services.file.dto.response.FileAssetCleanupResponse;

public interface FileAssetCleanupService {
    FileAssetCleanupResponse cleanupDueAssets();
}
