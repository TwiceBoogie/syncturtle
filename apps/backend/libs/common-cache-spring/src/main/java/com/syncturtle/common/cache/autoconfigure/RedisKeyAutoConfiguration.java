package com.syncturtle.common.cache.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.cache.properties.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;

@AutoConfiguration
@EnableConfigurationProperties(RedisKeyProperties.class)
public class RedisKeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedisKeyBuilder redisKeyBuilder(RedisKeyProperties properties) {
        return new RedisKeyBuilder(properties);
    }

}
