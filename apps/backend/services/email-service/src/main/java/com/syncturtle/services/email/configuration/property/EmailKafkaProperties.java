package com.syncturtle.services.email.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka")
public final class EmailKafkaProperties {

    private final boolean enabled;
    private final String consumerGroup;
    private final String configBroadcastGroup;

    public EmailKafkaProperties(Boolean enabled, String consumerGroup, String configBroadcastGroup) {
        Assert.notNull(enabled, "app.kafka.enabled is required");
        Assert.hasText(consumerGroup, "app.kafka.consumer-group is required");
        Assert.hasText(configBroadcastGroup, "app.kafka.config-broadcast-group is required");

        this.enabled = enabled;
        this.consumerGroup = consumerGroup;
        this.configBroadcastGroup = configBroadcastGroup;
    }

}
