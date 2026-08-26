package com.syncturtle.services.file.service.result;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FileUploadCreateResult {
    UUID assetId;
    String assetUrl;
    Instant uploadExpiresAt;
    PresignedPostResult uploadData;
}
