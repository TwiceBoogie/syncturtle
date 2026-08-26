package com.syncturtle.services.email.configuration.feign;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.email.configuration.property.InstanceServiceClientProperties;

import feign.Request;
import lombok.RequiredArgsConstructor;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class InstanceServiceFeignConfiguration {

    private final InstanceServiceClientProperties properties;

    @Bean
    Request.Options instanceServiceRequestOptions() {
        return new Request.Options(
                properties.getConnectTimeout().toMillis(),
                TimeUnit.MILLISECONDS,
                properties.getReadTimeout().toMillis(),
                TimeUnit.MILLISECONDS,
                true);
    }

}
