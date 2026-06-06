package com.syncturtle.services.workspace.configuration.cache.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.services.workspace.configuration.cache.WorkspaceFlagsRuntimeCacheNames;

@EnableCaching
@Configuration(proxyBeanMethods = false)
public class WorkspaceFlagsRuntimeCacheConfiguration {

    private static final String OWNER = "workspace-service";
    private static final String RESOURCE = "workspace-flags-runtime";
    private static final String SOURCE = "source";
    private static final String SOURCE_SERVICE = "instance-service";
    private static final String CONTRACT_VERSION = "v1";

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisKeyBuilder redisKeyBuilder) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        String keyPrefix = redisKeyBuilder.configKey(OWNER, RESOURCE, SOURCE, SOURCE_SERVICE, CONTRACT_VERSION) + ":";
        RedisCacheConfiguration workspaceFlagRuntimeCache = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> keyPrefix)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofHours(1));

        return builder -> builder.withCacheConfiguration(
                WorkspaceFlagsRuntimeCacheNames.WORKSPACE_FLAGS_RUNTIME,
                workspaceFlagRuntimeCache);
    }

}
