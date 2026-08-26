package com.syncturtle.services.email.configuration.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public final class KafkaListenerFactoryConfiguration {

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;
    private final CommonErrorHandler kafkaErrorHandler;
    private final ConcurrentKafkaListenerContainerFactoryConfigurer factoryConfigurer;

    public KafkaListenerFactoryConfiguration(KafkaProperties kafkaProperties, JsonMapper jsonMapper,
            CommonErrorHandler kafkaErrorHandler, ConcurrentKafkaListenerContainerFactoryConfigurer factoryConfigurer) {
        Assert.notNull(kafkaProperties, "kafkaProperties is required");
        Assert.notNull(jsonMapper, "jsonMapper is required");
        Assert.notNull(kafkaErrorHandler, "kafkaErrorHandler is required");
        Assert.notNull(factoryConfigurer, "factoryConfigurer is required");

        this.kafkaProperties = kafkaProperties;
        this.jsonMapper = jsonMapper;
        this.kafkaErrorHandler = kafkaErrorHandler;
        this.factoryConfigurer = factoryConfigurer;
    }

    @Bean
    ConsumerFactory<String, EmailToSendEvent> emailToSendConsumerFactory() {
        return typedConsumerFactory(EmailToSendEvent.class);
    }

    @Bean
    ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory() {
        return typedConsumerFactory(InstanceConfigurationEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, EmailToSendEvent> emailToSendKafkaListenerFactory(
            ConsumerFactory<String, EmailToSendEvent> emailToSendConsumerFactory) {
        return buildFactory(emailToSendConsumerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InstanceConfigurationEvent> instanceConfigurationKafkaListenerFactory(
            ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory) {
        return buildFactory(instanceConfigurationConsumerFactory);
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildFactory(
            ConsumerFactory<String, T> consumerFactory) {
        Assert.notNull(consumerFactory, "consumerFactory is required");

        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factoryConfigurer.configure((ConcurrentKafkaListenerContainerFactory) factory,
                (ConsumerFactory) consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

}
