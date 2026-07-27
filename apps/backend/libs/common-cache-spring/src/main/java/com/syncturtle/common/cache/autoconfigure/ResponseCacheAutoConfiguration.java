package com.syncturtle.common.cache.autoconfigure;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.properties.ResponseCacheProperties;
import com.syncturtle.common.cache.response.ResponseCacheAspect;
import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;
import com.syncturtle.common.web.autoconfigure.GatewayContextAutoConfiguration;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = {
        DataRedisAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        AopAutoConfiguration.class,
        GatewayContextAutoConfiguration.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({
        StringRedisTemplate.class,
        JsonMapper.class,
        Aspect.class,
        HttpServletRequest.class,
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
    @ConditionalOnBean({
            StringRedisTemplate.class,
            JsonMapper.class
    })
    @ConditionalOnMissingBean
    ResponseCacheAspect responseCacheAspect(
            StringRedisTemplate redis,
            JsonMapper jsonMapper,
            ResponseCacheProperties properties,
            ResponseCacheKeyBuilder keyBuilder,
            ObjectProvider<RequestUserContext> requestUserContextProvider,
            Environment environment) {
        String serviceName = environment.getProperty(
                "spring.application.name",
                "unknown-service");

        return new ResponseCacheAspect(
                redis,
                jsonMapper,
                properties,
                keyBuilder,
                requestUserContextProvider.getIfAvailable(),
                serviceName);
    }
}