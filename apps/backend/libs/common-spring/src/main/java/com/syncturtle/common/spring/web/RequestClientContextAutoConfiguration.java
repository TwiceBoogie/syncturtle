package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestClientContext;

@Configuration(proxyBeanMethods = false)
public class RequestClientContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestClientContext requestClientContext() {
        return new RequestClientContext();
    }

}
