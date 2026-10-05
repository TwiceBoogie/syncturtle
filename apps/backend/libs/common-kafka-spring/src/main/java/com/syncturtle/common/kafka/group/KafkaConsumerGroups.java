package com.syncturtle.common.kafka.group;

import com.syncturtle.common.kafka.property.KafkaDeploymentProperties;

public final class KafkaConsumerGroups {

    private final String deploymentId;

    public KafkaConsumerGroups(KafkaDeploymentProperties properties) {
        this.deploymentId = properties.getDeploymentId();
    }

    public String workGroupId(String service, String purpose) {
        return deploymentId + "." + KafkaDeploymentProperties.requireSegment(service)
                + "." + KafkaDeploymentProperties.requireSegment(purpose);
    }

}
