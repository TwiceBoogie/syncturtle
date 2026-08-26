package com.syncturtle.services.instance.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InstanceConfigurationPropertiesTest {

    @Nested
    class KafkaTopicPropertiesConstructor {

        @Test
        void acceptsPositivePartitionAndReplicaCounts() {
            KafkaTopicProperties properties = new KafkaTopicProperties(3, 1);

            assertThat(properties.getPartitions()).isEqualTo(3);
            assertThat(properties.getReplicas()).isEqualTo(1);
        }
    }

    @Nested
    class ProductTelemetryPropertiesConstructor {

        @Test
        void allowsNoEndpointWhenDisabled() {
            ProductTelemetryProperties properties = new ProductTelemetryProperties(false, null, null,
                    Duration.ofMinutes(10));

            assertThat(properties.getEndpoint()).isEmpty();
            assertThat(properties.getApiKey()).isEmpty();
        }
    }
}
