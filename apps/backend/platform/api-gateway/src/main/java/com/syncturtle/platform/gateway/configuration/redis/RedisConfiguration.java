package com.syncturtle.platform.gateway.configuration.redis;

import java.time.Duration;

import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedisConfiguration {

    @Bean
    LettuceClientConfigurationBuilderCustomizer lettuceTuning() {
        return builder -> builder
                .commandTimeout(Duration.ofMillis(200))
                .shutdownTimeout(Duration.ofMillis(100));
    }
}
