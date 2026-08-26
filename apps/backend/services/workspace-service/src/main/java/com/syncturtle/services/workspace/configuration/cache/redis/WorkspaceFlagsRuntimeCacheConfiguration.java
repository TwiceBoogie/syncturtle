package com.syncturtle.services.workspace.configuration.cache.redis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.services.workspace.configuration.cache.WorkspaceFlagsRuntimeCacheNames;

@EnableCaching
@Configuration(proxyBeanMethods = false)
public class WorkspaceFlagsRuntimeCacheConfiguration {

    private static final String OWNER = "workspace-service";
    private static final String RESOURCE = "workspace-flags-runtime";
    private static final String SOURCE = "source";
    private static final String SOURCE_SERVICE = "instance-service";
    private static final String CONTRACT_VERSION = "v2";

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisKeyBuilder redisKeyBuilder,
            @Value("${app.workspace.runtime-cache.ttl}") Duration ttl) {
        RedisSerializer<Object> serializer = GenericJacksonJsonRedisSerializer.builder().build();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> cachePrefix(redisKeyBuilder))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(ttl);

        return builder -> builder.withCacheConfiguration(WorkspaceFlagsRuntimeCacheNames.WORKSPACE_FLAGS_RUNTIME,
                configuration);
    }

    private static String cachePrefix(RedisKeyBuilder redisKeyBuilder) {
        return redisKeyBuilder.configKey(OWNER, RESOURCE, SOURCE, SOURCE_SERVICE, CONTRACT_VERSION) + ":";
    }

}
