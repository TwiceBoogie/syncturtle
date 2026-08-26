package com.syncturtle.services.file.service.collaborator.storage;

import java.time.Duration;
import java.util.Map;

import com.syncturtle.services.file.service.result.PresignedPostResult;

public interface ObjectStorageGateway {
    PresignedPostResult generatePresignedPost(String objectKey, String contentType, long maxSizeBytes,
            Duration expiration);

    String generatePresignedUrl(String objectKey, String versionId, Duration expiration, String disposition,
            String filename);

    Map<String, Object> getObjectMetadata(String objectKey);

    byte[] getObjectPrefix(String objectKey, String versionId, int maximumBytes);

    void deleteObjectVersions(String objectKey);
}
