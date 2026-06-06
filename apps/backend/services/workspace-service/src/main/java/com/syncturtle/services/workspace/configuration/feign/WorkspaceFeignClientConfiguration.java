package com.syncturtle.services.workspace.configuration.feign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.workspace.client.InstanceClient;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = InstanceClient.class)
public class WorkspaceFeignClientConfiguration {

}
