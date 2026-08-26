package com.syncturtle.services.file.configuration.property;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.file.upload")
public final class FileUploadProperties {

    private final DataSize maxImageSize;
    private final Duration idempotencyRetention;
    private final Duration idempotencyProcessingLease;

    public FileUploadProperties(
            @DefaultValue("5MB") DataSize maxImageSize,
            @DefaultValue("24h") Duration idempotencyRetention,
            @DefaultValue("1m") Duration idempotencyProcessingLease) {
        this.maxImageSize = requirePositive(maxImageSize, "maxImageSize");
        this.idempotencyRetention = requirePositive(idempotencyRetention, "idempotencyRetention");
        this.idempotencyProcessingLease = requirePositive(idempotencyProcessingLease,
                "idempotencyProcessingLease");

        if (idempotencyProcessingLease.compareTo(idempotencyRetention) >= 0) {
            throw new IllegalArgumentException(
                    "app.file.upload.idempotencyProcessingLease must be shorter than idempotencyRetention");
        }
    }

    private static DataSize requirePositive(DataSize value, String fieldName) {
        DataSize size = Objects.requireNonNull(value, "app.file.upload." + fieldName + " is required");
        if (size.toBytes() <= 0) {
            throw new IllegalArgumentException("app.file.upload." + fieldName + " must be positive");
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
