package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.web.client.FeignAuthErrorDecoder;
import com.syncturtle.common.web.client.FeignHeaderPropagation;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({
        RequestInterceptor.class,
        RequestContextHolder.class,
        ServletRequestAttributes.class,
        HttpServletRequest.class,
        FeignClient.class
})
public class FeignClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    FeignHeaderPropagation feignHeaderPropagation() {
        return new FeignHeaderPropagation();
    }

    @Bean(name = "syncturtleHeaderPropagationInterceptor")
    @ConditionalOnMissingBean(name = "syncturtleHeaderPropagationInterceptor")
    RequestInterceptor syncturtleHeaderPropagationInterceptor(FeignHeaderPropagation propagation) {
        return propagation.asRequestInterceptor();
    }

    @Bean
    @ConditionalOnBean(JsonMapper.class)
    @ConditionalOnMissingBean(ErrorDecoder.class)
    @ConditionalOnClass({ ErrorDecoder.class, JsonMapper.class })
    ErrorDecoder syncturtleFeignAuthErrorDecoder(JsonMapper mapper) {
        return new FeignAuthErrorDecoder(mapper);
    }

}
