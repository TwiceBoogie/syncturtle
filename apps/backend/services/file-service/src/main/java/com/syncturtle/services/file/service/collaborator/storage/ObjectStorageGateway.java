package com.syncturtle.services.file.service.collaborator.storage;

import java.time.Duration;
import java.util.Map;

import com.syncturtle.services.file.dto.response.PresignedPostResponse;

public interface ObjectStorageGateway {
    PresignedPostResponse generatePresignedPost(String objectKey, String contentType, long maxSizeBytes,
            Duration expiration);

    String generatePresignedUrl(String objectKey, Duration expiration, String disposition, String filename);

    Map<String, Object> getObjectMetadata(String objectKey);
}
