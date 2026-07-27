package com.syncturtle.services.email.configuration.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncturtle.services.email.configuration.property.EmailRuntimeConfigCacheProperties;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigSnapshot;

import lombok.RequiredArgsConstructor;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class EmailRuntimeConfigCacheConfiguration {

    private final EmailRuntimeConfigCacheProperties properties;

    @Bean
    Cache<String, EmailRuntimeConfigSnapshot> emailRuntimeConfigCache() {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(properties.getTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

}
