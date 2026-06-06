package com.syncturtle.services.user.configuration.feign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.user.client.InstanceClient;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = InstanceClient.class)
public class UserFeignClientConfiguration {

}
