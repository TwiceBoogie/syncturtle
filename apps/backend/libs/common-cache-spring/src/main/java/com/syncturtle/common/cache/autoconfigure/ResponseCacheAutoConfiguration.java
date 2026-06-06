package com.syncturtle.common.cache.autoconfigure;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.cache.properties.ResponseCacheProperties;
import com.syncturtle.common.cache.response.ResponseCacheAspect;
import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;
import com.syncturtle.common.web.autoconfigure.GatewayContextAutoConfiguration;
import com.syncturtle.common.web.context.RequestUserContext;

@AutoConfiguration(after = {
        RedisAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        AopAutoConfiguration.class,
        GatewayContextAutoConfiguration.class
})
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({
        StringRedisTemplate.class,
        ObjectMapper.class,
        Aspect.class,
        ResponseCacheAspect.class
})
@EnableConfigurationProperties(ResponseCacheProperties.class)
public class ResponseCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ResponseCacheKeyBuilder responseCacheKeyBuilder() {
        return new ResponseCacheKeyBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ StringRedisTemplate.class, ObjectMapper.class })
    ResponseCacheAspect responseCacheAspect(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            ResponseCacheProperties props,
            ResponseCacheKeyBuilder keyBuilder,
            ObjectProvider<RequestUserContext> requestUserContextProvider,
            Environment env) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");

        return new ResponseCacheAspect(
                redis,
                objectMapper,
                props,
                keyBuilder,
                requestUserContextProvider.getIfAvailable(),
                serviceName);
    }

}
