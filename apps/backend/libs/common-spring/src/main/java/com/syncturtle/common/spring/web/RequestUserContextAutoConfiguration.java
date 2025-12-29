package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.filters.GatewayUserHeaderFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RequestUserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestUserContext requestUserContext() {
        return new RequestUserContext();
    }

    @Bean
    FilterRegistrationBean<GatewayUserHeaderFilter> gatewayUserHeaderFilter(RequestUserContext ctx) {
        FilterRegistrationBean<GatewayUserHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new GatewayUserHeaderFilter(ctx));
        bean.setOrder(-50);
        return bean;
    }
}
