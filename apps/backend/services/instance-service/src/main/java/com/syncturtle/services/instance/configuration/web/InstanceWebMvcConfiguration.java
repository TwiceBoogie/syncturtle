package com.syncturtle.services.instance.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.configuration.property.AuthorizationInterceptorProperties;
import com.syncturtle.services.instance.configuration.web.authorization.AuthorizationInterceptor;
import com.syncturtle.services.instance.configuration.web.authorization.AuthorizationResponseWriter;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;

@Profile("!setup")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthorizationInterceptorProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InstanceWebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    AuthorizationResponseWriter authorizationResponseWriter() {
        return new AuthorizationResponseWriter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    AuthorizationInterceptor authorizationInterceptor(
            AuthorizationInterceptorProperties properties,
            RequestUserContext requestUserContext,
            InstanceAuthorizationService instanceAuthorizationService,
            AuthorizationResponseWriter responseWriter) {
        return new AuthorizationInterceptor(
                requestUserContext,
                instanceAuthorizationService,
                responseWriter,
                properties.isRequireAuthenticationByDefault());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    WebMvcConfigurer authorizationWebMvcConfigurer(
            AuthorizationInterceptorProperties properties,
            AuthorizationInterceptor authorizationInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authorizationInterceptor)
                        .order(properties.getOrder())
                        .addPathPatterns(properties.getPathPatterns())
                        .excludePathPatterns(properties.getExcludePathPatterns());
            }
        };
    }

}
