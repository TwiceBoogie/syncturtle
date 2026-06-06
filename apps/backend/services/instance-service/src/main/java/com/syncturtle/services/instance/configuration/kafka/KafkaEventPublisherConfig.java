package com.syncturtle.services.instance.configuration.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.services.instance.messaging.InstanceEventPublisher;

@Configuration(proxyBeanMethods = false)
public class KafkaEventPublisherConfig {

    @Bean
    @ConditionalOnMissingBean(InstanceEventPublisher.class)
    InstanceEventPublisher noopInstanceEventPublisher() {
        return new InstanceEventPublisher() {
            @Override
            public void publishInstanceEvent(InstanceEvent event) {
                // noop
            }
        };
    }

}
