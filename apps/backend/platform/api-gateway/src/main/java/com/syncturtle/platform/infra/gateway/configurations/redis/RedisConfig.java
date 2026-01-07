package com.syncturtle.platform.infra.gateway.configurations.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Bean
    LettuceClientConfigurationBuilderCustomizer lettuceTuning() {
        return builder -> builder
                .commandTimeout(Duration.ofMillis(200))
                .shutdownTimeout(Duration.ofMillis(100));
    }
}
