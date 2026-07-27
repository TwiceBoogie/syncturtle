package com.syncturtle.services.user.configuration.cache.redis;

import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.services.user.configuration.cache.UserAuthRuntimeCacheNames;

@EnableCaching
@Configuration(proxyBeanMethods = false)
public class UserAuthRuntimeCacheConfiguration {

    private static final String OWNER = "user-service";
    private static final String RESOURCE = "user-auth-runtime";
    private static final String SOURCE = "source";
    private static final String SOURCE_SERVICE = "instance-service";
    private static final String CONTRACT_VERSION = "v2";
    private static final Duration ENTRY_TTL = Duration.ofHours(1);

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisKeyBuilder redisKeyBuilder) {
        RedisSerializer<Object> serializer = GenericJacksonJsonRedisSerializer.builder().build();

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> cachePrefix(redisKeyBuilder))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(ENTRY_TTL);

        return builder -> builder.withCacheConfiguration(
                UserAuthRuntimeCacheNames.USER_AUTH_RUNTIME,
                cacheConfiguration);
    }

    private static String cachePrefix(RedisKeyBuilder redisKeyBuilder) {
        return redisKeyBuilder.configKey(
                OWNER,
                RESOURCE,
                SOURCE,
                SOURCE_SERVICE,
                CONTRACT_VERSION) + ":";
    }

}
