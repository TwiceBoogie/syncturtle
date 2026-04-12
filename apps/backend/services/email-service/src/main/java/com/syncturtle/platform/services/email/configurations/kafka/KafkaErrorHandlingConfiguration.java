package com.syncturtle.platform.services.email.configurations.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.ExponentialBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaErrorHandlingConfiguration {

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(8_000L);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> {
                    log.error("Kafka message exhausted retries. topic={}, key={}", rec.topic(), rec.key());
                    return new TopicPartition(rec.topic() + ".DLT", rec.partition());
                });

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.addNotRetryableExceptions(
                DeserializationException.class,
                MessageConversionException.class,
                ListenerExecutionFailedException.class);

        return handler;
    }

}
