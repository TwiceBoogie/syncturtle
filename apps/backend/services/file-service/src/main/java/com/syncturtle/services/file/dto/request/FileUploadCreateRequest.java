package com.syncturtle.services.file.dto.request;

import java.util.UUID;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FileUploadCreateRequest {
    @NotNull
    FileAssetPurpose purpose;
    private UUID workspaceId;
    @NotNull
    @Size(min = 1, max = 255)
    String originalFilename;
    @NotNull
    @Size(min = 1, max = 127)
    String contentType;
    @Min(1)
    long sizeBytes;
}
