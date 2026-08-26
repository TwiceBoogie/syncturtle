package com.syncturtle.common.kafka.group;

import com.syncturtle.common.kafka.property.InstanceConfigurationConsumerGroupProperties;

import lombok.Getter;

@Getter
public final class InstanceConfigurationConsumerGroups {
    private final String localGroupId;
    private final String sharedGroupId;

    public InstanceConfigurationConsumerGroups(String serviceName,
            InstanceConfigurationConsumerGroupProperties properties) {
        String service = InstanceConfigurationConsumerGroupProperties.requireSegment(serviceName, "service-name");
        String prefix = properties.getDeploymentId() + "." + service + ".instance-config.";
        this.localGroupId = prefix + "local." + properties.getReplicaId();
        this.sharedGroupId = prefix + "shared";
    }
}
