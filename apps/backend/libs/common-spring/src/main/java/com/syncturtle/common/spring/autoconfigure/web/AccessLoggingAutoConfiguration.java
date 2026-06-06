package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.spring.autoconfigure.observability.logging.AccessLogInterceptor;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({
        DispatcherServlet.class,
        HandlerInterceptor.class,
        WebMvcConfigurer.class,
        AccessLogInterceptor.class
})
@ConditionalOnProperty(prefix = "app.web.access-logging", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AccessLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AccessLogInterceptor accessLogInterceptor() {
        return new AccessLogInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(name = "accessLogWebMvcConfigurer")
    WebMvcConfigurer accessLogWebMvcConfigurer(AccessLogInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).order(Ordered.LOWEST_PRECEDENCE);
            }
        };
    }
}
