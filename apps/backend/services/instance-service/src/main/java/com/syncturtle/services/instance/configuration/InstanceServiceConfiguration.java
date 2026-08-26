package com.syncturtle.services.instance.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.instance.configuration.property.InstanceServiceProperties;
import com.syncturtle.services.instance.configuration.property.ProductTelemetryProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ InstanceServiceProperties.class, ProductTelemetryProperties.class })
public class InstanceServiceConfiguration {

}
