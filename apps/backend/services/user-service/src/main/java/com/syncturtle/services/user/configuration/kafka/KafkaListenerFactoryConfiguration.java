package com.syncturtle.services.user.configuration.kafka;

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

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;

@Configuration(proxyBeanMethods = false)
public class KafkaListenerFactoryConfiguration {

    @Bean
    ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return typedConsumerFactory(kafkaProperties, InstanceConfigurationEvent.class);
    }

    @Bean
    ConsumerFactory<String, InstanceEvent> instanceEventConsumerFactory(KafkaProperties kafkaProperties) {
        return typedConsumerFactory(kafkaProperties, InstanceEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InstanceConfigurationEvent> instanceConfigurationKafkaListenerFactory(
            ConsumerFactory<String, InstanceConfigurationEvent> instanceConfigurationConsumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        return buildFactory(instanceConfigurationConsumerFactory, kafkaErrorHandler);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InstanceEvent> instanceKafkaListenerFactory(
            ConsumerFactory<String, InstanceEvent> instanceConsumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        return buildFactory(instanceConsumerFactory, kafkaErrorHandler);
    }

    private <T> ConsumerFactory<String, T> typedConsumerFactory(KafkaProperties kafkaProperties, Class<T> valueType) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        removeJsonDeserializerProperties(props);
        JsonDeserializer<T> delegate = new JsonDeserializer<>(valueType);
        delegate.addTrustedPackages("com.syncturtle.*");
        delegate.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<T> valueDeserializer = new ErrorHandlingDeserializer<>(delegate);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer);
    }

    private static void removeJsonDeserializerProperties(Map<String, Object> props) {
        props.remove(JsonDeserializer.TRUSTED_PACKAGES);
        props.remove(JsonDeserializer.USE_TYPE_INFO_HEADERS);
        props.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        props.remove(JsonDeserializer.KEY_DEFAULT_TYPE);
        props.remove(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS);
        props.remove(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS);

        props.keySet().removeIf(key -> key instanceof String stringKey
                && stringKey.startsWith("spring.json."));
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
