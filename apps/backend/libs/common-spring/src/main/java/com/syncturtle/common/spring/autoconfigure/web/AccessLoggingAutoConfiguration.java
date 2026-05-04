package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.spring.autoconfigure.observability.logging.AccessLogInterceptor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class AccessLoggingAutoConfiguration {

    @Bean
    AccessLogInterceptor accessLogInterceptor() {
        return new AccessLogInterceptor();
    }

    @Bean
    WebMvcConfigurer accessLogWebMvcConfigurer(AccessLogInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).order(Ordered.LOWEST_PRECEDENCE);
            }
        };
    }
}
