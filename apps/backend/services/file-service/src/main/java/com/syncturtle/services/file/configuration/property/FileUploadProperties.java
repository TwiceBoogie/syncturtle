package com.syncturtle.services.file.configuration.property;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import com.syncturtle.services.file.policy.FileLifecyclePolicy;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.file.upload")
public final class FileUploadProperties {

    private static final DataSize MAX_SUPPORTED_IMAGE_SIZE = DataSize.ofMegabytes(100);

    private final DataSize maxImageSize;
    private final Duration idempotencyProcessingLease;

    public FileUploadProperties(
            @DefaultValue("5MB") DataSize maxImageSize,
            @DefaultValue("1m") Duration idempotencyProcessingLease) {
        this.maxImageSize = requirePositive(maxImageSize, "maxImageSize");
        this.idempotencyProcessingLease = requirePositive(idempotencyProcessingLease,
                "idempotencyProcessingLease");

        if (idempotencyProcessingLease.compareTo(FileLifecyclePolicy.UPLOAD_IDEMPOTENCY_RETENTION) >= 0) {
            throw new IllegalArgumentException(
                    "app.file.upload.idempotencyProcessingLease must be shorter than the 24h replay horizon");
        }
    }

    private static DataSize requirePositive(DataSize value, String fieldName) {
        DataSize size = Objects.requireNonNull(value, "app.file.upload." + fieldName + " is required");
        if (size.toBytes() <= 0) {
            throw new IllegalArgumentException("app.file.upload." + fieldName + " must be positive");
        }

        if (size.toBytes() > MAX_SUPPORTED_IMAGE_SIZE.toBytes()) {
            throw new IllegalArgumentException("app.file.upload." + fieldName + " must not exceed 100MB");
        }

        return size;
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        Duration duration = Objects.requireNonNull(value, "app.file.upload." + fieldName + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("app.file.upload." + fieldName + " must be positive");
        }

        return duration;
    }
}
