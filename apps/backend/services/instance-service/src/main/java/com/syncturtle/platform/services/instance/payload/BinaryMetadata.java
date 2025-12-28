package com.syncturtle.platform.services.instance.payload;

import java.time.Instant;

import org.springframework.boot.info.BuildProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BinaryMetadata {

    private final String binaryVersion;
    private final Instant binaryBuiltAt;

    public static BinaryMetadata from(BuildProperties build, Instant now) {
        String version = (build != null) ? build.getVersion() : "0.0.0.0";
        Instant builtAt = (build != null && build.getTime() != null) ? build.getTime() : now;

        return new BinaryMetadata(version, builtAt);
    }

}
