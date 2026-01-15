package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestUserContext;

@Configuration(proxyBeanMethods = false)
public class RequestUserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestUserContext requestUserContext() {
        return new RequestUserContext();
    }

}
