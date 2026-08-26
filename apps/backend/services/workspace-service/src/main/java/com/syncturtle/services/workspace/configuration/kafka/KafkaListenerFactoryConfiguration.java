package com.syncturtle.services.workspace.configuration.kafka;

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

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;

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
    ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationEventConsumerFactory() {
        return typedConsumerFactory(InstanceConfigurationEvent.class);
    }

    @Bean
    ConsumerFactory<String, UserEvent> userEventConsumerFactory() {
        return typedConsumerFactory(UserEvent.class);
    }

    @Bean
    ConsumerFactory<String, UserAuthenticatedEvent> userAuthenticatedEventConsumerFactory() {
        return typedConsumerFactory(UserAuthenticatedEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InstanceConfigurationEvent> instanceConfigurationKafkaListenerFactory(
            ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory) {
        return buildFactory(instanceConfigurationConsumerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, UserEvent> userKafkaListenerFactory(
            ConsumerFactory<String, UserEvent> userConsumerFactory) {
        return buildFactory(userConsumerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, UserAuthenticatedEvent> userAuthenticatedKafkaListenerFactory(
            ConsumerFactory<String, UserAuthenticatedEvent> userAuthenticatedConsumerFactory) {
        return buildFactory(userAuthenticatedConsumerFactory);
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

        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

}
