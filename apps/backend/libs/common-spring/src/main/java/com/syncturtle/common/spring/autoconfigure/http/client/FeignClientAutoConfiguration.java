package com.syncturtle.common.spring.autoconfigure.http.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.web.client.FeignAuthErrorDecoder;
import com.syncturtle.common.web.client.FeignHeaderPropagation;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({
        RequestInterceptor.class,
        RequestContextHolder.class,
        ServletRequestAttributes.class,
        HttpServletRequest.class,
        FeignClient.class })
public class FeignClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    FeignHeaderPropagation feignHeaderPropagation() {
        return new FeignHeaderPropagation();
    }

    @Bean(name = "syncturtleHeaderPropagationInterceptor")
    @ConditionalOnMissingBean(name = "syncturtleHeaderPropagationInterceptor")
    RequestInterceptor syncturtleHeaderPropagationRequestInterceptor(
            FeignHeaderPropagation propagation) {
        return propagation.asRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    @ConditionalOnClass({ ErrorDecoder.class, ObjectMapper.class })
    ErrorDecoder syncturtleFeignAuthErrorDecoder(ObjectMapper mapper) {
        return new FeignAuthErrorDecoder(mapper);
    }

}
