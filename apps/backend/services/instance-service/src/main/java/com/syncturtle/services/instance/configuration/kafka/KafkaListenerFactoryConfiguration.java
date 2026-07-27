package com.syncturtle.services.instance.configuration.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class KafkaListenerFactoryConfiguration {

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;
    private final CommonErrorHandler kafkaErrorHandler;

    public KafkaListenerFactoryConfiguration(KafkaProperties kafkaProperties, JsonMapper jsonMapper,
            CommonErrorHandler kafkaErrorHandler) {
        Assert.notNull(kafkaProperties, "kafkaProperties is required");
        Assert.notNull(jsonMapper, "jsonMapper is required");
        Assert.notNull(kafkaErrorHandler, "kafkaErrorHandler is required");

        this.kafkaProperties = kafkaProperties;
        this.jsonMapper = jsonMapper;
        this.kafkaErrorHandler = kafkaErrorHandler;
    }

    @Bean
    ConsumerFactory<String, UserEvent> userEventConsumerFactory() {
        return typedConsumerFactory(UserEvent.class);
    }

    @Bean
    ConsumerFactory<String, WorkspaceEvent> workspaceEventConsumerFactory() {
        return typedConsumerFactory(WorkspaceEvent.class);
    }

    @Bean
    ConsumerFactory<String, WorkspaceMemberEvent> workspaceMemberEventConsumerFactory() {
        return typedConsumerFactory(WorkspaceMemberEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, UserEvent> userKafkaListenerFactory(
            ConsumerFactory<String, UserEvent> userConsumerFactory) {
        return buildFactory(userConsumerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WorkspaceEvent> workspaceKafkaListenerFactory(
            ConsumerFactory<String, WorkspaceEvent> workspaceConsumerFactory) {
        return buildFactory(workspaceConsumerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WorkspaceMemberEvent> workspaceMemberKafkaListenerFactory(
            ConsumerFactory<String, WorkspaceMemberEvent> workspaceMemberConsumerFactory) {
        return buildFactory(workspaceMemberConsumerFactory);
    }

    private <T> ConsumerFactory<String, T> typedConsumerFactory(Class<T> valueType) {
        Assert.notNull(valueType, "valueType is required");

        Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties());

        removeProgrammaticDeserializerProperties(consumerProperties);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                StringDeserializer::new,
                () -> createValueDeserializer(valueType),
                false);
    }

    private <T> ErrorHandlingDeserializer<T> createValueDeserializer(Class<T> valueType) {
        Assert.notNull(valueType, "valueType is required");

        JacksonJsonDeserializer<T> delegate = new JacksonJsonDeserializer<>(valueType, jsonMapper, false);

        return new ErrorHandlingDeserializer<>(delegate);
    }

    private static void removeProgrammaticDeserializerProperties(Map<String, Object> consumerProperties) {
        Assert.notNull(consumerProperties, "consumerProperties is required");

        consumerProperties.remove(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        consumerProperties.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        consumerProperties.remove(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS);
        consumerProperties.remove(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS);

        consumerProperties.keySet().removeIf(key -> key.startsWith("spring.json."));
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildFactory(
            ConsumerFactory<String, T> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

}
