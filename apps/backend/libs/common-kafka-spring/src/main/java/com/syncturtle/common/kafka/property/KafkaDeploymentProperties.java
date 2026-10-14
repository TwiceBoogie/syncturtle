package com.syncturtle.common.kafka.property;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka")
public final class KafkaDeploymentProperties {

    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final String deploymentId;

    public KafkaDeploymentProperties(String deploymentId) {
        this.deploymentId = requireSegment(deploymentId);
    }

    public static String requireSegment(String value) {
        Assert.hasText(value, "Kafka identity segment is required");

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        Assert.isTrue(SEGMENT.matcher(normalized).matches(),
                "Kafka identity segment must contain only lowercase letters, digits, and hyphens");

        return normalized;
    }

}
