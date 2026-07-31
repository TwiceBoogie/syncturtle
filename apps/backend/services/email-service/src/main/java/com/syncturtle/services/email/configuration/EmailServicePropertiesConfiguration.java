package com.syncturtle.services.email.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.configuration.property.EmailKafkaProperties;
import com.syncturtle.services.email.configuration.property.EmailRuntimeConfigCacheProperties;
import com.syncturtle.services.email.configuration.property.EmailTransportProperties;
import com.syncturtle.services.email.configuration.property.InstanceServiceClientProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ EmailInboxProperties.class, EmailKafkaProperties.class,
        EmailRuntimeConfigCacheProperties.class, EmailTransportProperties.class,
        InstanceServiceClientProperties.class
})
public final class EmailServicePropertiesConfiguration {

}
