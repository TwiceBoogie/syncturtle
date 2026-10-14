package com.syncturtle.services.user.configuration.web;

import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.user.security.authorization.UserAuthorizationInterceptor;

@Configuration(proxyBeanMethods = false)
public class UserWebMvcAuthorizationConfiguration {

    @Bean
    UserAuthorizationInterceptor userAuthorizationInterceptor(RequestUserContext requestUserContext) {
        return new UserAuthorizationInterceptor(requestUserContext);
    }

    @Bean
    WebMvcConfigurer authorizationWebMvcConfigurer(UserAuthorizationInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .order(Ordered.HIGHEST_PRECEDENCE + 100)
                        .addPathPatterns("/api/**", "/auth/**");
            }
        };
    }

}
