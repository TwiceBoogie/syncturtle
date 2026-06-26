package com.syncturtle.services.user.dto.response;

import java.util.UUID;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileAssetValidationResponse {
    UUID id;
    UUID workspaceId;
    UUID ownerUserId;
    FileAssetPurpose purpose;
    boolean uploaded;
    boolean deleted;
}
