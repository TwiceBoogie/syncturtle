package com.syncturtle.services.email.configuration.property;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SyncturtleConfig.class)
public class EmailServiceProperties {

}
