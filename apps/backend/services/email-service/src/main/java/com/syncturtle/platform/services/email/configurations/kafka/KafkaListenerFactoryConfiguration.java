package com.syncturtle.platform.services.email.configurations.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.syncturtle.common.core.events.EmailToSendEvent;
import com.syncturtle.common.core.events.InstanceConfigurationEvent;

@Configuration(proxyBeanMethods = false)
public class KafkaListenerFactoryConfiguration {

    @Bean
    ConsumerFactory<String, EmailToSendEvent> emailToSendConsumerFactory(KafkaProperties kafkaProperties) {
        return typedConsumerFactory(kafkaProperties, EmailToSendEvent.class);
    }

    @Bean
    ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory(
            KafkaProperties kafkaProperties) {
        return typedConsumerFactory(kafkaProperties, InstanceConfigurationEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, EmailToSendEvent> emailToSendKafkaListenerFactory(
            ConsumerFactory<String, EmailToSendEvent> emailToSendConsumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        return buildFactory(emailToSendConsumerFactory, kafkaErrorHandler);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InstanceConfigurationEvent> instanceConfigurationKafkaListenerFactory(
            ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        return buildFactory(instanceConfigurationConsumerFactory, kafkaErrorHandler);
    }

    private <T> ConsumerFactory<String, T> typedConsumerFactory(KafkaProperties kafkaProperties, Class<T> valueType) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        JsonDeserializer<T> delegate = new JsonDeserializer<>(valueType);
        delegate.addTrustedPackages("com.syncturtle.*");
        delegate.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<T> valueDeserializer = new ErrorHandlingDeserializer<>(delegate);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildFactory(
            ConsumerFactory<String, T> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

}
