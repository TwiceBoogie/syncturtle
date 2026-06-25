package com.syncturtle.services.file.service;

import java.time.Duration;
import java.util.Map;

import com.syncturtle.services.file.dto.response.PresignedPostData;

public interface S3StorageService {
    PresignedPostData generatePresignedPost(String objectKey, String contentType, long maxSizeBytes,
            Duration expiration);

    String generatePresignedUrl(String objectKey, Duration expiration, String disposition, String filename);

    Map<String, Object> getObjectMetadata(String objectKey);
}
