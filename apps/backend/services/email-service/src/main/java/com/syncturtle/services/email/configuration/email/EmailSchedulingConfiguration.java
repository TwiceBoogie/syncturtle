package com.syncturtle.services.email.configuration.email;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class EmailSchedulingConfiguration {

}
