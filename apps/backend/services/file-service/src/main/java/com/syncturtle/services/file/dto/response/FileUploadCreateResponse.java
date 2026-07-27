package com.syncturtle.services.file.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileUploadCreateResponse {
    UUID assetId;
    String assetUrl;
    Instant uploadExpiresAt;
    PresignedPostData uploadData;
}
