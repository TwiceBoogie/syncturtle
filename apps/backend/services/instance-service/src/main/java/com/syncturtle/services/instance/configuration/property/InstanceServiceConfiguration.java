package com.syncturtle.services.instance.configuration.property;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ InstanceServiceProperties.class, ProductTelemetryProperties.class })
public class InstanceServiceConfiguration {

}
