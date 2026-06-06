package com.syncturtle.services.instance.model.param;

import java.time.Instant;

import org.springframework.boot.info.BuildProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class InstanceBinaryParam {

    private static final String DEFAULT_VERSION = "0.0.0";

    private final String binaryVersion;
    private final Instant binaryBuiltAt;

    private InstanceBinaryParam(String binaryVersion, Instant binaryBuiltAt) {
        Assert.hasText(binaryVersion, "binaryVersion is required");
        Assert.notNull(binaryBuiltAt, "binaryBuiltAt is required");

        this.binaryVersion = binaryVersion;
        this.binaryBuiltAt = binaryBuiltAt;
    }

    public static InstanceBinaryParam from(BuildProperties build, Instant fallbackTime) {
        Assert.notNull(fallbackTime, "fallbackTime is required");

        String version = build != null && hasText(build.getVersion())
                ? build.getVersion()
                : DEFAULT_VERSION;

        Instant builtAt = build != null && build.getTime() != null
                ? build.getTime()
                : fallbackTime;

        return new InstanceBinaryParam(version, builtAt);
    }

    public static InstanceBinaryParam of(String binaryVersion, Instant binaryBuiltAt) {
        return new InstanceBinaryParam(binaryVersion, binaryBuiltAt);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
