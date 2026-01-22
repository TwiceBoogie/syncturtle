package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.filters.GatewayClientHeaderFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(GatewayClientHeaderFilter.class)
public class GatewayClientHeaderFilterAutoConfiguration {

    @Bean
    FilterRegistrationBean<GatewayClientHeaderFilter> gatewayClientHeaderFilter(RequestClientContext clientContext) {
        FilterRegistrationBean<GatewayClientHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new GatewayClientHeaderFilter(clientContext));
        bean.setOrder(-49);
        return bean;
    }

}
