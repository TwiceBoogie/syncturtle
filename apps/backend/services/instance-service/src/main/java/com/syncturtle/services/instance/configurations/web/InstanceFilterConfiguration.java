package com.syncturtle.services.instance.configurations.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.syncturtle.common.spring.web.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;
import com.syncturtle.common.web.csrf.CsrfTokenService;
import com.syncturtle.services.instance.configurations.properties.FormCsrfFilterProperties;
import com.syncturtle.services.instance.configurations.web.filters.FormCsrfOncePerRequestFilter;

import jakarta.servlet.DispatcherType;

@Profile("!setup")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FormCsrfFilterProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InstanceFilterConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.csrf.form", name = "enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<FormCsrfOncePerRequestFilter> formCsrfOncePerRequestFilterRegistration(
            FormCsrfFilterProperties properties,
            CsrfTokenService csrfTokenService,
            ServletAuthCookieWriter cookieWriter,
            PublicUrlBuilder hostResolver) {
        FormCsrfOncePerRequestFilter filter = new FormCsrfOncePerRequestFilter(
                properties.getProtectedEndpoints(),
                csrfTokenService,
                cookieWriter,
                hostResolver);

        FilterRegistrationBean<FormCsrfOncePerRequestFilter> registration = new FilterRegistrationBean<>();

        registration.setName("formCsrfOncePerRequestFilter");
        registration.setFilter(filter);
        registration.setEnabled(properties.isEnabled());
        registration.setOrder(properties.getOrder());
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setUrlPatterns(properties.getProtectedEndpoints());

        return registration;
    }

}
