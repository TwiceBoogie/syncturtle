package com.syncturtle.services.user.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.user.configuration.property.AuthorizationInterceptorProperties;
import com.syncturtle.services.user.security.authorization.UserAuthorizationInterceptor;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthorizationInterceptorProperties.class)
public class UserWebMvcAuthorizationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    UserAuthorizationInterceptor userAuthorizationInterceptor(AuthorizationInterceptorProperties properties,
            RequestUserContext requestUserContext) {
        return new UserAuthorizationInterceptor(requestUserContext, properties.isRequireAuthenticationByDefault());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    WebMvcConfigurer authorizationWebMvcConfigurer(AuthorizationInterceptorProperties properties,
            UserAuthorizationInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .order(properties.getOrder())
                        .addPathPatterns(properties.getPathPatterns())
                        .excludePathPatterns(properties.getExcludePathPatterns());
            }
        };
    }

}
