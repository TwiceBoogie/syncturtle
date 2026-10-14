package com.syncturtle.services.instance.configuration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.listener.CommonErrorHandler;

import com.syncturtle.common.kafka.topic.KafkaDeploymentTopics;
import com.syncturtle.services.instance.messaging.kafka.consumer.KafkaUserEventConsumer;
import com.syncturtle.services.instance.messaging.kafka.consumer.KafkaWorkspaceEventConsumer;
import com.syncturtle.services.instance.messaging.kafka.consumer.KafkaWorkspaceMemberEventConsumer;
import com.syncturtle.services.instance.messaging.kafka.mapper.UserEventMapper;
import com.syncturtle.services.instance.messaging.kafka.mapper.WorkspaceEventMapper;
import com.syncturtle.services.instance.messaging.kafka.mapper.WorkspaceMemberEventMapper;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.WorkspaceMemberLiteRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("Instance Kafka profile graph")
class SetupKafkaProfileTest {

    @Nested
    class Setup {

        @Test
        void excludesConsumersAndFactoriesButKeepsTopicDefinitions() {
            runner("setup").run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBeansOfType(KafkaUserEventConsumer.class)).isEmpty();
                assertThat(context.getBeansOfType(KafkaWorkspaceEventConsumer.class)).isEmpty();
                assertThat(context.getBeansOfType(KafkaWorkspaceMemberEventConsumer.class)).isEmpty();
                assertThat(context.getBeansOfType(KafkaListenerFactoryConfiguration.class)).isEmpty();
                assertThat(context.containsBean("userKafkaListenerFactory")).isFalse();
                assertThat(context.containsBean("workspaceKafkaListenerFactory")).isFalse();
                assertThat(context.containsBean("workspaceMemberKafkaListenerFactory")).isFalse();
                assertThat(context.getBeansOfType(NewTopic.class)).hasSize(18);
            });
        }
    }

    @Nested
    class NormalRuntime {

        @Test
        void registersAllConsumersAndTypedFactoriesWithoutSetupTopics() {
            runner("local").run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBeansOfType(KafkaUserEventConsumer.class)).hasSize(1);
                assertThat(context.getBeansOfType(KafkaWorkspaceEventConsumer.class)).hasSize(1);
                assertThat(context.getBeansOfType(KafkaWorkspaceMemberEventConsumer.class)).hasSize(1);
                assertThat(context.getBeansOfType(KafkaListenerFactoryConfiguration.class)).hasSize(1);
                assertThat(context.containsBean("userKafkaListenerFactory")).isTrue();
                assertThat(context.containsBean("workspaceKafkaListenerFactory")).isTrue();
                assertThat(context.containsBean("workspaceMemberKafkaListenerFactory")).isTrue();
                assertThat(context.getBeansOfType(NewTopic.class)).isEmpty();
            });
        }
    }

    private ApplicationContextRunner runner(String profile) {
        KafkaDeploymentTopics topics = mock(KafkaDeploymentTopics.class);
        when(topics.topic(anyString())).thenAnswer(invocation -> "test." + invocation.getArgument(0));

        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withPropertyValues("app.kafka.topics.partitions=1", "app.kafka.topics.replicas=1")
                .withUserConfiguration(
                        KafkaListenerFactoryConfiguration.class,
                        KafkaTopicConfiguration.class,
                        KafkaUserEventConsumer.class,
                        KafkaWorkspaceEventConsumer.class,
                        KafkaWorkspaceMemberEventConsumer.class)
                .withBean(KafkaProperties.class, KafkaProperties::new)
                .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
                .withBean(CommonErrorHandler.class, () -> mock(CommonErrorHandler.class))
                .withBean(KafkaDeploymentTopics.class, () -> topics)
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(UserRepository.class, () -> mock(UserRepository.class))
                .withBean(WorkspaceRepository.class, () -> mock(WorkspaceRepository.class))
                .withBean(WorkspaceMemberLiteRepository.class,
                        () -> mock(WorkspaceMemberLiteRepository.class))
                .withBean(UserEventMapper.class, () -> mock(UserEventMapper.class))
                .withBean(WorkspaceEventMapper.class, () -> mock(WorkspaceEventMapper.class))
                .withBean(WorkspaceMemberEventMapper.class,
                        () -> mock(WorkspaceMemberEventMapper.class));
    }

}
