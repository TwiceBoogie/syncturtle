package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.services.file.dto.response.FileUploadCreateResponse;
import com.syncturtle.services.file.service.param.FileUploadCreateParam;

public interface FileUploadService {
    FileUploadCreateResponse createUpload(FileUploadCreateParam param);

    void completeUpload(UUID currentUserId, UUID assetId);

    void deleteAsset(UUID currentUserId, UUID assetId);
}
