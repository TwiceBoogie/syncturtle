package com.syncturtle.platform.services.instance.configurations.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.events.InstanceEvent;
import com.syncturtle.platform.services.instance.messaging.InstanceEventPublisher;

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
