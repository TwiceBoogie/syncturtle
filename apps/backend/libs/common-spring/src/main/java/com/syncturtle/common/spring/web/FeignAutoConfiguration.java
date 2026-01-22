package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.web.client.FeignAuthErrorDecoder;
import com.syncturtle.common.web.client.FeignHeaderPropagation;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(FeignClient.class)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    FeignHeaderPropagation feignHeaderPropagation() {
        return new FeignHeaderPropagation();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncturtleHeaderPropagationInterceptor")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({ RequestInterceptor.class, HttpServletRequest.class })
    RequestInterceptor syncturtleHeaderPropagationInterceptor(FeignHeaderPropagation propagation) {
        return propagation.asRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    @ConditionalOnClass({ ErrorDecoder.class, ObjectMapper.class })
    ErrorDecoder syncturtleFeignAuthErrorDecoder(ObjectMapper mapper) {
        return new FeignAuthErrorDecoder(mapper);
    }

}
