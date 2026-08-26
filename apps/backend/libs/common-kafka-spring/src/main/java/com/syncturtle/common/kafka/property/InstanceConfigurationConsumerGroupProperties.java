package com.syncturtle.common.kafka.property;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka.instance-configuration")
public final class InstanceConfigurationConsumerGroupProperties {

    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final String deploymentId;
    private final String replicaId;
    private final int localReplicaCount;

    public InstanceConfigurationConsumerGroupProperties(String deploymentId, String replicaId,
            @DefaultValue("1") int localReplicaCount) {
        this.deploymentId = requireSegment(deploymentId, "deployment-id");
        this.replicaId = requireSegment(replicaId, "replica-id");
        if (localReplicaCount < 1) {
            throw new IllegalArgumentException(
                    "app.kafka.instance-configuration.local-replica-count must be at least 1");
        }
        if (localReplicaCount > 1 && "single".equals(replicaId)) {
            throw new IllegalArgumentException(
                    "app.kafka.instance-configuration.replica-id must be unique when local-replica-count is greater than 1");
        }
        this.localReplicaCount = localReplicaCount;
    }

    public static String requireSegment(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.kafka.instance-configuration." + propertyName + " is required");
        }
        String normalized = value.trim().toLowerCase();
        if (!SEGMENT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "app.kafka.instance-configuration." + propertyName
                            + " must contain only lowercase letters, digits, and hyphens");
        }
        return normalized;
    }
}
