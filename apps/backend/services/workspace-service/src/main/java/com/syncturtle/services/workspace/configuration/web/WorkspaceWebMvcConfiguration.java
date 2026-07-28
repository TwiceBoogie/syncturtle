package com.syncturtle.services.workspace.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.workspace.configuration.property.AuthorizationInterceptorProperties;
import com.syncturtle.services.workspace.configuration.web.authorization.WorkspaceAuthorizationInterceptor;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthorizationInterceptorProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WorkspaceWebMvcConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    WorkspaceAuthorizationInterceptor workspaceAuthorizationInterceptor(AuthorizationInterceptorProperties properties,
            RequestUserContext requestUserContext, WorkspaceAuthorizationService workspaceAuthorizationService) {
        return new WorkspaceAuthorizationInterceptor(requestUserContext, workspaceAuthorizationService,
                properties.isRequireAuthenticationByDefault());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.web.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    WebMvcConfigurer authorizationWebMvcConfigurer(AuthorizationInterceptorProperties properties,
            WorkspaceAuthorizationInterceptor interceptor) {
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
