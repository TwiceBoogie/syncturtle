package com.syncturtle.platform.services.user.configurations.cache.caffeine;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncturtle.platform.services.user.dto.internal.UserAuthRuntimeSecretConfig;

@Configuration
public class UserAuthRuntimeSecretCacheConfiguration {

    @Bean
    Cache<String, UserAuthRuntimeSecretConfig> userAuthRuntimeSecretCache() {
        return Caffeine.newBuilder()
                .maximumSize(4)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

}
