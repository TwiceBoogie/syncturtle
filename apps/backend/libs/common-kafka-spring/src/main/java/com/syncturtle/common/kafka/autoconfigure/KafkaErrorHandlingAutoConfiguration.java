package com.syncturtle.common.kafka.autoconfigure;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import com.syncturtle.common.kafka.properties.KafkaErrorHandlingProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass({
        KafkaTemplate.class,
        CommonErrorHandler.class,
        DefaultErrorHandler.class,
        DeadLetterPublishingRecoverer.class
})
@ConditionalOnBooleanProperty(prefix = "app.kafka", name = "enabled")
@EnableConfigurationProperties(KafkaErrorHandlingProperties.class)
public class KafkaErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            KafkaErrorHandlingProperties properties) {
        ExponentialBackOff backOff = createBackOff(properties);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> resolveDeadLetterTopic(
                        record,
                        properties));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.setAckAfterHandle(
                properties.isAckAfterHandle());

        handler.setCommitRecovered(
                properties.isCommitRecovered());

        handler.addNotRetryableExceptions(
                IllegalArgumentException.class);

        if (properties.isLogRetries()) {
            handler.setRetryListeners(
                    (record, exception, deliveryAttempt) -> log.warn(
                            "Kafka listener retry "
                                    + "attempt={} "
                                    + "topic={} "
                                    + "partition={} "
                                    + "offset={} "
                                    + "key={} "
                                    + "error={}",
                            deliveryAttempt,
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.key(),
                            exception.toString()));
        }

        return handler;
    }

    private static ExponentialBackOff createBackOff(
            KafkaErrorHandlingProperties properties) {
        ExponentialBackOff backOff = new ExponentialBackOff(
                properties
                        .getInitialInterval()
                        .toMillis(),
                properties.getMultiplier());

        backOff.setMaxElapsedTime(
                properties
                        .getMaxElapsedTime()
                        .toMillis());

        return backOff;
    }

    private static TopicPartition resolveDeadLetterTopic(
            ConsumerRecord<?, ?> record,
            KafkaErrorHandlingProperties properties) {
        String deadLetterTopic = record.topic() + properties.getDltSuffix();

        return new TopicPartition(
                deadLetterTopic,
                record.partition());
    }
}