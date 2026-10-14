package com.syncturtle.services.instance.version;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.services.instance.configuration.property.BinaryVersionProperties;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentVersionResolver {

    private static final String DEVELOPMENT_VERSION = "development";

    private final BinaryVersionProperties properties;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public InstanceBinaryParam resolve(Instant fallbackTime) {
        Assert.notNull(fallbackTime, "fallbackTime is required");

        BuildProperties build = buildPropertiesProvider.getIfAvailable();
        Instant builtAt = build != null && build.getTime() != null ? build.getTime() : fallbackTime;

        String override = properties.getVersion();
        if (StringUtils.hasText(override)) {
            String version = override.trim();
            if (ReleaseVersion.parse(version).isPresent()) {
                return InstanceBinaryParam.of(version, builtAt);
            }
            log.warn("Ignoring malformed app.binary.version override");
        }

        if (build != null && StringUtils.hasText(build.getVersion())) {
            String version = build.getVersion().trim();
            if (ReleaseVersion.parse(version).isPresent()) {
                return InstanceBinaryParam.of(version, builtAt);
            }
            log.warn("Ignoring malformed build.version");
        }

        return InstanceBinaryParam.of(DEVELOPMENT_VERSION, builtAt);
    }

}
