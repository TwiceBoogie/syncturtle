package com.syncturtle.services.instance.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.security.authorization.InstanceAuthorizationInterceptor;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;

@Profile("!setup")
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InstanceWebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    InstanceAuthorizationInterceptor authorizationInterceptor(
            RequestUserContext requestUserContext,
            InstanceAuthorizationService instanceAuthorizationService) {
        return new InstanceAuthorizationInterceptor(requestUserContext, instanceAuthorizationService);
    }

    @Bean
    WebMvcConfigurer authorizationWebMvcConfigurer(
            InstanceAuthorizationInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .order(Ordered.HIGHEST_PRECEDENCE + 100)
                        .addPathPatterns("/api/**");
            }
        };
    }

}
