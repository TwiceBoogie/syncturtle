package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.filter.GatewayClientHeaderFilter;
import com.syncturtle.common.web.filter.GatewayUserHeaderFilter;
import com.syncturtle.common.web.properties.GatewayContextProperties;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

@AutoConfiguration
@ConditionalOnClass({ RequestUserContext.class, RequestClientContext.class })
@EnableConfigurationProperties(GatewayContextProperties.class)
public class GatewayContextAutoConfiguration {

    private static final int USER_HEADER_FILTER_ORDER = -50;
    private static final int CLIENT_HEADER_FILTER_ORDER = -49;

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({
            Filter.class,
            OncePerRequestFilter.class,
            GatewayUserHeaderFilter.class,
            GatewayClientHeaderFilter.class })
    @ConditionalOnProperty(prefix = "app.gateway.context", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class GatewayContextFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "gatewayUserHeaderFilterRegistration")
        FilterRegistrationBean<GatewayUserHeaderFilter> gatewayUserHeaderFilterRegistration(
                RequestUserContext requestUserContext,
                GatewayContextProperties properties) {
            FilterRegistrationBean<GatewayUserHeaderFilter> registration = new FilterRegistrationBean<>();

            registration.setName("gatewayUserHeaderFilter");
            registration.setFilter(new GatewayUserHeaderFilter(requestUserContext));
            registration.setOrder(USER_HEADER_FILTER_ORDER);
            registration.setDispatcherTypes(DispatcherType.REQUEST);
            registration.addUrlPatterns("/*");

            return registration;
        }

        @Bean
        @ConditionalOnMissingBean(name = "gatewayClientHeaderFilterRegistration")
        FilterRegistrationBean<GatewayClientHeaderFilter> gatewayClientHeaderFilterRegistration(
                RequestClientContext requestClientContext,
                GatewayContextProperties properties) {
            FilterRegistrationBean<GatewayClientHeaderFilter> registration = new FilterRegistrationBean<>();

            registration.setName("gatewayClientHeaderFilter");
            registration.setFilter(new GatewayClientHeaderFilter(requestClientContext));
            registration.setOrder(CLIENT_HEADER_FILTER_ORDER);
            registration.setDispatcherTypes(DispatcherType.REQUEST);
            registration.addUrlPatterns("/*");

            return registration;
        }

    }

}
