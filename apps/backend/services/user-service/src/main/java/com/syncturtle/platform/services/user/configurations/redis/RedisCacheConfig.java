package com.syncturtle.platform.services.user.configurations.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

// https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.redis
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("s2s:instance:configuration",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .disableCachingNullValues()
                                .prefixCacheNameWith("syncturtle:")
                                .entryTtl(Duration.ofHours(1l))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer())));
    }

}
