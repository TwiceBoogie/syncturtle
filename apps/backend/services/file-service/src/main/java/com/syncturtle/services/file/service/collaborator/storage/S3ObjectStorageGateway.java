package com.syncturtle.services.file.service.collaborator.storage;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.syncturtle.services.file.configuration.property.StorageProperties;
import com.syncturtle.services.file.service.result.PresignedPostResult;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class S3ObjectStorageGateway implements ObjectStorageGateway {

    private static final String AWS4_HMAC_SHA256 = "AWS4-HMAC-SHA256";
    private static final String SERVICE_NAME = "s3";
    private static final String AWS4_REQUEST = "aws4_request";
    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    public PresignedPostResult generatePresignedPost(String objectKey, String contentType, long maxSizeBytes,
            Duration expiration) {
        Assert.hasText(objectKey, "objectKey is required");
        Assert.hasText(contentType, "contentType is required");
        Assert.isTrue(maxSizeBytes > 0, "maxSizeBytes must be greater than 0");

        Duration effectiveExpiration = resolveExpiration(expiration);
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(effectiveExpiration);
        String amzDate = AMZ_DATE_FORMAT.format(now);
        String dateStamp = DATE_STAMP_FORMAT.format(now);
        String credential = credential(dateStamp);

        Map<String, String> fields = buildPostFields(objectKey, contentType, amzDate, credential);
        List<Object> conditions = buildPostConditions(objectKey, contentType, maxSizeBytes, amzDate, credential);
        String policy = encodePolicy(expiresAt, conditions);
        String signature = signPolicy(policy, dateStamp);

        fields.put("policy", policy);
        fields.put("x-amz-signature", signature);

        return new PresignedPostResult(resolveFormAction(), fields);
    }

    @Override
    public String generatePresignedUrl(String objectKey, String versionId, Duration expiration, String disposition,
            String filename) {
        Assert.hasText(objectKey, "objectKey is required");
        Assert.hasText(versionId, "versionId is required");

        Duration effectiveExpiration = resolveExpiration(expiration);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .versionId(versionId)
                .responseContentDisposition(contentDisposition(disposition, filename))
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(effectiveExpiration)
                .getObjectRequest(request)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException exception) {
            throw new IllegalStateException("Failed to generate S3 presigned URL", exception);
        }
    }

    @Override
    public Map<String, Object> getObjectMetadata(String objectKey) {
        Assert.hasText(objectKey, "objectKey is required");

        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("ContentType", response.contentType());
            metadata.put("ContentLength", response.contentLength());
            metadata.put("LastModified", response.lastModified() == null ? null : response.lastModified().toString());
            metadata.put("ETag", response.eTag());
            metadata.put("VersionId", response.versionId());
            metadata.put("Metadata", response.metadata());

            return metadata;
        } catch (SdkException exception) {
            throw new IllegalStateException("Failed to fetch S3 object metadata. objectKey=" + objectKey, exception);
        }
    }

    @Override
    public byte[] getObjectPrefix(String objectKey, String versionId, int maximumBytes) {
        Assert.hasText(objectKey, "objectKey is required");
        Assert.hasText(versionId, "versionId is required");
        Assert.isTrue(maximumBytes > 0, "maximumBytes must be greater than 0");

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .versionId(versionId)
                    .range("bytes=0-" + (maximumBytes - 1))
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (SdkException exception) {
            throw new IllegalStateException("Failed to read S3 object prefix. objectKey=" + objectKey
                    + " versionId=" + versionId, exception);
        }
    }

    @Override
    public void deleteObjectVersions(String objectKey) {
        Assert.hasText(objectKey, "objectKey is required");

        try {
            String keyMarker = null;
            String versionIdMarker = null;
            boolean truncated;

            do {
                ListObjectVersionsRequest.Builder request = ListObjectVersionsRequest.builder()
                        .bucket(properties.getBucket())
                        .prefix(objectKey);

                if (StringUtils.hasText(keyMarker)) {
                    request.keyMarker(keyMarker);
                }

                if (StringUtils.hasText(versionIdMarker)) {
                    request.versionIdMarker(versionIdMarker);
                }

                ListObjectVersionsResponse response = s3Client.listObjectVersions(request.build());
                List<ObjectIdentifier> objects = new ArrayList<>();

                for (ObjectVersion version : response.versions()) {
                    if (objectKey.equals(version.key())) {
                        objects.add(ObjectIdentifier.builder()
                                .key(version.key())
                                .versionId(version.versionId())
                                .build());
                    }
                }

                for (DeleteMarkerEntry marker : response.deleteMarkers()) {
                    if (objectKey.equals(marker.key())) {
                        objects.add(ObjectIdentifier.builder()
                                .key(marker.key())
                                .versionId(marker.versionId())
                                .build());
                    }
                }

                if (!objects.isEmpty()) {
                    DeleteObjectsResponse deletion = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(properties.getBucket())
                            .delete(Delete.builder().objects(objects).quiet(true).build())
                            .build());
                    if (!deletion.errors().isEmpty()) {
                        throw new IllegalStateException("S3 reported one or more object-version deletion failures. "
                                + "objectKey=" + objectKey + " failures=" + deletion.errors().size());
                    }
                }

                truncated = Boolean.TRUE.equals(response.isTruncated());
                keyMarker = response.nextKeyMarker();
                versionIdMarker = response.nextVersionIdMarker();
            } while (truncated);
        } catch (SdkException exception) {
            throw new IllegalStateException("Failed to delete S3 object versions. objectKey=" + objectKey, exception);
        }
    }

    private Map<String, String> buildPostFields(String objectKey, String contentType, String amzDate,
            String credential) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", objectKey);
        fields.put("Content-Type", contentType);
        fields.put("x-amz-algorithm", AWS4_HMAC_SHA256);
        fields.put("x-amz-credential", credential);
        fields.put("x-amz-date", amzDate);

        return fields;
    }

    private List<Object> buildPostConditions(String objectKey, String contentType, long maxSizeBytes, String amzDate,
            String credential) {
        List<Object> conditions = new ArrayList<>();
        conditions.add(Map.of("bucket", properties.getBucket()));
        conditions.add(Map.of("key", objectKey));
        conditions.add(Map.of("Content-Type", contentType));
        conditions.add(Map.of("x-amz-algorithm", AWS4_HMAC_SHA256));
        conditions.add(Map.of("x-amz-credential", credential));
        conditions.add(Map.of("x-amz-date", amzDate));
        conditions.add(List.of("content-length-range", 1, maxSizeBytes));

        return conditions;
    }

    private String encodePolicy(Instant expiresAt, List<Object> conditions) {
        Map<String, Object> policyDocument = new LinkedHashMap<>();
        policyDocument.put("expiration", DateTimeFormatter.ISO_INSTANT.format(expiresAt));
        policyDocument.put("conditions", conditions);

        try {
            String policyJson = jsonMapper.writeValueAsString(policyDocument);
            return Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize S3 POST policy.", exception);
        }
    }

    private String signPolicy(String policyBase64, String dateStamp) {
        try {
            byte[] signingKey = signatureKey(properties.getSecretKey(), dateStamp, properties.getRegion(),
                    SERVICE_NAME);
            byte[] signature = hmacSha256(signingKey, policyBase64.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign S3 POST policy.", exception);
        }
    }

    private String credential(String dateStamp) {
        return properties.getAccessKey()
                + "/" + dateStamp
                + "/" + properties.getRegion()
                + "/" + SERVICE_NAME
                + "/" + AWS4_REQUEST;
    }

    private String resolveFormAction() {
        return properties.getClientUploadEndpoint().toString();
    }

    private Duration resolveExpiration(Duration expiration) {
        if (expiration != null) {
            return expiration;
        }

        return properties.getPresignExpiration();
    }

    private String contentDisposition(String disposition, String filename) {
        String effectiveDisposition = StringUtils.hasText(disposition) ? disposition.trim() : "inline";
        String effectiveFilename = StringUtils.hasText(filename) ? filename.trim() : UUID.randomUUID().toString();
        String encodedFilename = UriUtils.encode(effectiveFilename, StandardCharsets.UTF_8);

        return effectiveDisposition + "; filename*=UTF-8''" + encodedFilename;
    }

    private static byte[] signatureKey(String secretKey, String dateStamp, String regionName, String serviceName)
            throws GeneralSecurityException {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(kDate, regionName.getBytes(StandardCharsets.UTF_8));
        byte[] kService = hmacSha256(kRegion, serviceName.getBytes(StandardCharsets.UTF_8));

        return hmacSha256(kService, AWS4_REQUEST.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));

        return mac.doFinal(data);
    }

}
