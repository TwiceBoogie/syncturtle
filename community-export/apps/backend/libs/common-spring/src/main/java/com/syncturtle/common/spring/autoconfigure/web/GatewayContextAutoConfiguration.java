package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.filter.GatewayClientHeaderFilter;
import com.syncturtle.common.web.filter.GatewayUserHeaderFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ GatewayUserHeaderFilter.class, GatewayClientHeaderFilter.class })
public class GatewayContextAutoConfiguration {

    @Bean
    FilterRegistrationBean<GatewayUserHeaderFilter> gatewayUserHeaderFilter(RequestUserContext userContext) {
        FilterRegistrationBean<GatewayUserHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new GatewayUserHeaderFilter(userContext));
        bean.setOrder(-50);
        return bean;
    }

    @Bean
    FilterRegistrationBean<GatewayClientHeaderFilter> gatewayClientHeaderFilter(RequestClientContext clientContext) {
        FilterRegistrationBean<GatewayClientHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new GatewayClientHeaderFilter(clientContext));
        bean.setOrder(-49);
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean
    RequestClientContext requestClientContext() {
        return new RequestClientContext();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestUserContext requestUserContext() {
        return new RequestUserContext();
    }

}
