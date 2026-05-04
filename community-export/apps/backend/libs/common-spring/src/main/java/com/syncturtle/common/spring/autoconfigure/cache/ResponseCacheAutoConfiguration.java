package com.syncturtle.common.spring.autoconfigure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.spring.cache.response.ResponseCacheAspect;
import com.syncturtle.common.spring.cache.response.ResponseCacheKeyBuilder;
import com.syncturtle.common.spring.properties.ResponseCacheProperties;
import com.syncturtle.common.web.context.RequestUserContext;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ StringRedisTemplate.class })
@EnableConfigurationProperties(ResponseCacheProperties.class)
public class ResponseCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ResponseCacheKeyBuilder responseCacheKeyBuilder() {
        return new ResponseCacheKeyBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    ResponseCacheAspect responseCacheAspect(StringRedisTemplate redis, ObjectMapper objectMapper,
            ResponseCacheProperties props, ResponseCacheKeyBuilder keyBuilder, RequestUserContext requestUserContext,
            Environment env) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        return new ResponseCacheAspect(redis, objectMapper, props, keyBuilder, requestUserContext, serviceName);
    }
}
