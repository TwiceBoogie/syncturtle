package com.syncturtle.services.email.configurations.email;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class EmailSchedulingConfiguration {

}
