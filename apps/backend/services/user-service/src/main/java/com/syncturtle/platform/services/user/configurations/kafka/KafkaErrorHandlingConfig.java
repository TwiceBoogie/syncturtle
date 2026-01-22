package com.syncturtle.platform.services.user.configurations.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaErrorHandlingConfig {

    @Bean
    CommonErrorHandler kafkErrorHandler(KafkaTemplate<Object, Object> template) {
        ExponentialBackOff backoff = new ExponentialBackOff(500L, 2.0);
        backoff.setMaxElapsedTime(8_000L);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));

        return new DefaultErrorHandler(recoverer, backoff);
    }

}
