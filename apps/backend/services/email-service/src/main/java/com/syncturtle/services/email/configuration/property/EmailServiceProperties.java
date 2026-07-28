package com.syncturtle.services.email.configuration.property;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ EmailInboxProperties.class, EmailKafkaProperties.class,
        EmailRuntimeConfigCacheProperties.class, EmailTransportProperties.class, InstanceServiceClientProperties.class
})
public final class EmailServiceProperties {

}
