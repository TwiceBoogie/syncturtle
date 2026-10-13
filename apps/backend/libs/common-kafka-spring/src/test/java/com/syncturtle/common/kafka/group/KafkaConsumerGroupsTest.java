package com.syncturtle.common.kafka.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import com.syncturtle.common.kafka.autoconfigure.InstanceConfigurationConsumerGroupAutoConfiguration;
import com.syncturtle.common.kafka.autoconfigure.KafkaConsumerGroupAutoConfiguration;
import com.syncturtle.common.kafka.property.InstanceConfigurationConsumerGroupProperties;
import com.syncturtle.common.kafka.property.KafkaDeploymentProperties;

class KafkaConsumerGroupsTest {
    @Test
    void workReplicasShareAGroupAndInstallationsRemainIsolated() {
        KafkaConsumerGroups replicaOne = new KafkaConsumerGroups(new KafkaDeploymentProperties("installation-a"));
        KafkaConsumerGroups replicaTwo = new KafkaConsumerGroups(new KafkaDeploymentProperties("installation-a"));
        KafkaConsumerGroups otherInstallation = new KafkaConsumerGroups(new KafkaDeploymentProperties("installation-b"));

        String group = replicaOne.workGroupId("email-service", "dispatch-v1");

        assertThat(group).isEqualTo("installation-a.email-service.dispatch-v1");
        assertThat(replicaTwo.workGroupId("email-service", "dispatch-v1")).isEqualTo(group);
        assertThat(otherInstallation.workGroupId("email-service", "dispatch-v1")).isNotEqualTo(group);
        assertThat(replicaOne.workGroupId("file-service", "user-asset-links-v1")).isNotEqualTo(group);
    }

    @Test
    void localInvalidationReachesEveryReplicaAndSharedInvalidationUsesOneGroup() {
        InstanceConfigurationConsumerGroups first = new InstanceConfigurationConsumerGroups("user-service",
                new InstanceConfigurationConsumerGroupProperties("installation-a", "replica-1", 2));
        InstanceConfigurationConsumerGroups second = new InstanceConfigurationConsumerGroups("user-service",
                new InstanceConfigurationConsumerGroupProperties("installation-a", "replica-2", 2));

        assertThat(first.getLocalGroupId()).isNotEqualTo(second.getLocalGroupId());
        assertThat(first.getSharedGroupId()).isEqualTo(second.getSharedGroupId());
    }

    @Test
    void rejectsNormalizedSingleReplicaFallbackWhenMultipleReplicasAreDeclared() {
        assertThatThrownBy(() -> new InstanceConfigurationConsumerGroupProperties("installation-a", "SINGLE", 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InstanceConfigurationConsumerGroupProperties("installation-a", " single ", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresAnInstallationIdentity() {
        assertThatThrownBy(() -> new KafkaDeploymentProperties(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KafkaDeploymentProperties("bad.identity"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void workGroupsAreConfiguredWhenKafkaInfrastructureExists() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KafkaConsumerGroupAutoConfiguration.class))
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .withPropertyValues("app.kafka.deployment-id=installation-a")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KafkaConsumerGroups.class);
                });
    }

    @Test
    void nonKafkaApplicationsDoNotRequireAnInstallationIdentity() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KafkaConsumerGroupAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KafkaDeploymentProperties.class);
                });
    }

    @Test
    void instanceConfigurationParticipantsReceiveRequiredGroupsWithoutAnEnableSwitch() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InstanceConfigurationConsumerGroupAutoConfiguration.class))
                .withBean("instanceConfigurationKafkaListenerFactory", Object.class, Object::new)
                .withPropertyValues("spring.application.name=user-service",
                        "app.kafka.instance-configuration.deployment-id=installation-a",
                        "app.kafka.instance-configuration.replica-id=replica-1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(InstanceConfigurationConsumerGroups.class);
                });
    }

    @Test
    void nonParticipantsDoNotBindReplicaConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InstanceConfigurationConsumerGroupAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(InstanceConfigurationConsumerGroupProperties.class);
                });
    }
}
