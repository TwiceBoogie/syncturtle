package com.syncturtle.services.instance.model.param;

import java.time.Instant;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class InstanceBinaryParam {

    private final String binaryVersion;
    private final Instant binaryBuiltAt;

    private InstanceBinaryParam(String binaryVersion, Instant binaryBuiltAt) {
        Assert.hasText(binaryVersion, "binaryVersion is required");
        Assert.notNull(binaryBuiltAt, "binaryBuiltAt is required");

        this.binaryVersion = binaryVersion;
        this.binaryBuiltAt = binaryBuiltAt;
    }

    public static InstanceBinaryParam of(String binaryVersion, Instant binaryBuiltAt) {
        return new InstanceBinaryParam(binaryVersion, binaryBuiltAt);
    }

}
