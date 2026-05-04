package com.syncturtle.services.email.configurations.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncturtle.services.email.configurations.properties.SyncturtleConfig;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;

@Configuration(proxyBeanMethods = false)
public class EmailRuntimeConfigCacheConfiguration {

    @Bean
    Cache<String, EmailRuntimeConfig> emailRuntimeConfigCache(SyncturtleConfig syncturtleConfig) {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(syncturtleConfig.getEmail().getCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

}
