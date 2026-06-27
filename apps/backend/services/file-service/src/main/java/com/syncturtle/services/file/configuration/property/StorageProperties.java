package com.syncturtle.services.file.configuration.property;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.storage")
public final class StorageProperties {

    private static final Pattern BUCKET_NAME_PATTERN = Pattern.compile(
            "^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");

    private static final Duration MIN_PRESIGN_EXPIRATION = Duration.ofSeconds(1);
    private static final Duration MAX_PRESIGN_EXPIRATION = Duration.ofDays(7);

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String bucket;
    /**
     * Backend-facing S3 endpoint.
     * 
     * Local java process -> Docker minio: http://localhost:9000
     * 
     * Docker file-service -> Docker minio: http://minio:9000
     * 
     * AWS S3: null
     */
    private final URI endpoint;
    /**
     * Browser-facing S3 endpoint.
     * 
     * Local nginx: http://localhost
     * 
     * Production Traefik: https://storage.syncturtle.com or
     * https://api.syncturtle.com
     */
    private final URI publicEndpoint;
    private final boolean usePathStyle;
    private final Duration presignExpiration;

    public StorageProperties(
            String accessKey,
            String secretKey,
            @DefaultValue("us-east-1") String region,
            String bucket,
            URI endpoint,
            URI publicEndpoint,
            @DefaultValue("true") boolean usePathStyle,
            @DefaultValue("1h") Duration presignExpiration) {
        this.accessKey = requireText(accessKey, "accessKey");
        this.secretKey = requireText(secretKey, "secretKey");
        this.region = requireText(region, "region");
        this.bucket = requireBucket(bucket);
        this.endpoint = normalizeRootEndpoint(endpoint, "endpoint");
        this.publicEndpoint = normalizeRootEndpoint(publicEndpoint, "publicEndpoint");
        this.usePathStyle = usePathStyle;
        this.presignExpiration = requirePresignExpiration(presignExpiration);
    }

    public long presignedExpirationSeconds() {
        return presignExpiration.getSeconds();
    }

    /**
     * Endpoint used by S3Presigner
     * 
     * <p>
     * Presigned URLs are returned to the browser, so this must be browser-reachable
     * 
     * @return
     */
    public URI getPresignEndpoint() {
        if (publicEndpoint != null) {
            return publicEndpoint;
        }

        return endpoint;
    }

    public URI getClientUploadEndpoint() {
        URI base = publicEndpoint != null ? publicEndpoint : endpoint;

        if (base == null) {
            return URI.create("https://" + bucket + ".s3." + region + ".amazonaws.com");
        }

        return URI.create(trimTrailingSlash(base.toString()) + "/" + bucket);
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.storage." + fieldName + " is required");
        }

        return value.trim();
    }

    private static String requireBucket(String value) {
        String bucket = requireText(value, "bucket");

        if (!BUCKET_NAME_PATTERN.matcher(bucket).matches()) {
            throw new IllegalArgumentException("app.storage.bucket must be an S3-compatible bucket name: " + bucket);
        }

        if (bucket.contains("..")) {
            throw new IllegalArgumentException("app.storage.bucket must not contain consecutive dots: " + bucket);
        }

        if (bucket.contains(".-") || bucket.contains("-.")) {
            throw new IllegalArgumentException(
                    "app.storage.bucket must not contain dot-dash or dash-dot sequences: " + bucket);
        }

        return bucket;
    }

    private static URI normalizeRootEndpoint(URI endpoint, String fieldName) {
        if (endpoint == null) {
            return null;
        }

        if (!endpoint.isAbsolute()) {
            throw new IllegalArgumentException("app.storage." + fieldName + " must be absolute");
        }

        String scheme = endpoint.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("app.storage." + fieldName + " must be http or https");
        }

        if (!StringUtils.hasText(endpoint.getHost())) {
            throw new IllegalArgumentException("app.storage." + fieldName + " must include a host");
        }

        if (StringUtils.hasText(endpoint.getRawQuery())) {
            throw new IllegalArgumentException("app.storage." + fieldName + " must not include a query string");
        }

        if (StringUtils.hasText(endpoint.getRawFragment())) {
            throw new IllegalArgumentException("app.storage." + fieldName + " must not include a fragment");
        }

        String path = endpoint.getRawPath();

        if (StringUtils.hasText(path) && !"/".equals(path)) {
            throw new IllegalArgumentException("app.storage." + fieldName
                    + " must not include a path. Put the bucket in app.storage.bucket instead.");
        }

        return URI.create(trimTrailingSlash(endpoint.toString()));
    }

    private static Duration requirePresignExpiration(Duration value) {
        Duration duration = Objects.requireNonNull(value, "app.storage.presignExpiration is required");

        if (duration.compareTo(MIN_PRESIGN_EXPIRATION) < 0) {
            throw new IllegalArgumentException(
                    "app.storage.presignExpiration must be at least " + MIN_PRESIGN_EXPIRATION);
        }

        if (duration.compareTo(MAX_PRESIGN_EXPIRATION) > 0) {
            throw new IllegalArgumentException(
                    "app.storage.presignExpiration must not be greater than " + MAX_PRESIGN_EXPIRATION);
        }

        return duration;
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

}
