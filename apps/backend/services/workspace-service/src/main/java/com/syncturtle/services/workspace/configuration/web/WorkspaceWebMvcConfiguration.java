package com.syncturtle.services.workspace.configuration.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.workspace.security.authorization.WorkspaceAuthorizationInterceptor;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WorkspaceWebMvcConfiguration {

    @Bean
    WorkspaceAuthorizationInterceptor workspaceAuthorizationInterceptor(
            RequestUserContext requestUserContext, WorkspaceAuthorizationService workspaceAuthorizationService) {
        return new WorkspaceAuthorizationInterceptor(requestUserContext, workspaceAuthorizationService);
    }

    @Bean
    WebMvcConfigurer authorizationWebMvcConfigurer(WorkspaceAuthorizationInterceptor interceptor) {
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
