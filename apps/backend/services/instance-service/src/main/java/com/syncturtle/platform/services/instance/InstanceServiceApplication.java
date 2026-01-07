package com.syncturtle.platform.services.instance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.syncturtle.platform.services.instance.configurations.properties.InstanceServiceProperties;
import com.syncturtle.platform.services.instance.configurations.properties.ProductTelemetryProperties;

@EnableFeignClients
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({ InstanceServiceProperties.class, ProductTelemetryProperties.class })
public class InstanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstanceServiceApplication.class, args);
    }
}
