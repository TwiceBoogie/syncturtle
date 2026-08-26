package com.syncturtle.services.user.configuration.cache.caffeine;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSecretConfig;

@Configuration(proxyBeanMethods = false)
public class UserAuthRuntimeSecretCacheConfiguration {

    @Bean
    Cache<String, UserAuthRuntimeSecretConfig> userAuthRuntimeSecretCache(
            @Value("${app.cache.runtime-secret-cache.ttl}") Duration ttl) {
        return Caffeine.newBuilder()
                .maximumSize(4)
                .expireAfterWrite(ttl)
                .build();
    }

}
