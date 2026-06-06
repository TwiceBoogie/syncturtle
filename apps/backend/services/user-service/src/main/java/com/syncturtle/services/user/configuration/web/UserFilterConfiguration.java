package com.syncturtle.services.user.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.configuration.property.FormCsrfFilterProperties;
import com.syncturtle.services.user.configuration.web.filter.FormCsrfOncePerRequestFilter;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FormCsrfFilterProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserFilterConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.csrf.form", name = "enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<FormCsrfOncePerRequestFilter> formCsrfOncePerRequestFilterRegistration(
            FormCsrfFilterProperties properties,
            CsrfTokenService csrfTokenService,
            ServletAuthCookieWriter cookieWriter,
            PublicUrlResolver hostResolver) {
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
