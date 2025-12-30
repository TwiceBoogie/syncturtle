package com.syncturtle.platform.services.instance.configurations.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Profile("!setup")
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthorizationInterceptor authz;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authz).addPathPatterns("/api/**");
    }
}
