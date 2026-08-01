package com.syncturtle.services.instance.configuration.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;

@Configuration(proxyBeanMethods = false)
public class RedisObservabilityConfiguration {

    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources(ObservationRegistry observationRegistry) {
        return DefaultClientResources.builder()
                .tracing(new MicrometerTracing(observationRegistry, "redis"))
                .build();
    }

}
