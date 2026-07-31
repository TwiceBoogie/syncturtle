package com.syncturtle.common.contracts.file.response;

import java.util.UUID;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FileAssetValidationResponse {
    UUID id;
    UUID workspaceId;
    UUID ownerUserId;
    FileAssetPurpose purpose;
    boolean uploaded;
    boolean deleted;
}
