package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.services.file.dto.request.FileUploadCreateRequest;
import com.syncturtle.services.file.dto.response.FileUploadCreateResponse;

public interface FileUploadService {
    FileUploadCreateResponse createUpload(UUID currentUserId, String idempotencyKey, FileUploadCreateRequest request);

    void completeUpload(UUID currentUserId, UUID assetId);

    void deleteAsset(UUID currentUserId, UUID assetId);
}
