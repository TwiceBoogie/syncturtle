package com.syncturtle.common.kafka.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.syncturtle.common.kafka.property.KafkaDeploymentProperties;
import com.syncturtle.common.kafka.topic.KafkaDeadLetterTopics;
import com.syncturtle.common.kafka.topic.KafkaDeploymentTopics;

class KafkaDeploymentTopicsTest {
    @Test
    void isolatesInstallationsSharingABrokerAndPreservesTheLogicalVersionAndDltSuffix() {
        KafkaDeploymentTopics first = new KafkaDeploymentTopics(new KafkaDeploymentProperties("installation-a"));
        KafkaDeploymentTopics second = new KafkaDeploymentTopics(new KafkaDeploymentProperties("installation-b"));

        assertThat(first.topic("email.events.v1")).isEqualTo("installation-a.email.events.v1");
        assertThat(second.topic("email.events.v1")).isNotEqualTo(first.topic("email.events.v1"));
        assertThat(first.topic("email.events.v1") + KafkaDeadLetterTopics.SUFFIX)
                .isEqualTo("installation-a.email.events.v1.DLT");
    }

    @Test
    void rejectsMissingTopicsAndNamesTooLongToCreateTheirDlt() {
        KafkaDeploymentTopics topics = new KafkaDeploymentTopics(new KafkaDeploymentProperties("installation-a"));

        assertThatThrownBy(() -> topics.topic("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> topics.topic("x".repeat(249))).isInstanceOf(IllegalArgumentException.class);
    }
}
