package com.syncturtle.common.spring.autoconfigure.observability.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.ExponentialBackOff;

import com.syncturtle.common.spring.properties.KafkaErrorHandlingProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnClass({
        KafkaTemplate.class,
        CommonErrorHandler.class,
        DefaultErrorHandler.class,
        DeadLetterPublishingRecoverer.class
})
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaErrorHandlingProperties.class)
public class KafkaErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            KafkaErrorHandlingProperties properties) {
        ExponentialBackOff backoff = new ExponentialBackOff(
                properties.getInitialInterval().toMillis(),
                properties.getMultiplier());

        backoff.setMaxElapsedTime(properties.getMaxElapsedTime().toMillis());

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> resolveDeadLetterTopic(record, properties));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        handler.setAckAfterHandle(properties.isAckAfterHandle());
        handler.setCommitRecovered(properties.isCommitRecovered());

        // failures mean the record is structurally bad, not that kafka, postgres, redis
        // or others is temp unavailable
        // retrying them usually burns time and pollutes logs so send to DLT quickly
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                MessageConversionException.class,
                IllegalArgumentException.class,
                ClassCastException.class);

        if (properties.isLogRetries()) {
            handler.setRetryListeners((record, exception, deliveryAttempt) -> log.warn(
                    "Kafka listener retry attempt={} topic={} partition={} offset={} key={} error={}",
                    deliveryAttempt,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    exception.toString()));
        }

        return handler;
    }

    private static TopicPartition resolveDeadLetterTopic(
            ConsumerRecord<?, ?> record,
            KafkaErrorHandlingProperties properties) {
        return new TopicPartition(
                record.topic() + properties.getDltSuffix(),
                record.partition());
    }

}
