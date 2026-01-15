package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.filters.GatewayUserHeaderFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(GatewayUserHeaderFilter.class)
public class GatewayUserHeaderFilterAutoConfiguration {

    @Bean
    FilterRegistrationBean<GatewayUserHeaderFilter> gatewayUserHeaderFilter(RequestUserContext userContext) {
        FilterRegistrationBean<GatewayUserHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new GatewayUserHeaderFilter(userContext));
        bean.setOrder(-50);
        return bean;
    }
}
