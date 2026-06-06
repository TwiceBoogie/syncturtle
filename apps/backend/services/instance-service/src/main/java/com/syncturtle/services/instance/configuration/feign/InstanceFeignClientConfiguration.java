package com.syncturtle.services.instance.configuration.feign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.instance.client.UserClient;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = UserClient.class)
public class InstanceFeignClientConfiguration {

}
