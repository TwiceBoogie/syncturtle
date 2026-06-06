package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpResponse;

import com.syncturtle.common.spring.properties.SecurityCookieProperties;
import com.syncturtle.common.spring.web.cookie.ReactiveCsrfCookieWriter;
import com.syncturtle.common.spring.web.cookie.SecurityCookieFactory;
import com.syncturtle.common.spring.web.cookie.ServletAuthCookieWriter;

import jakarta.servlet.http.HttpServletResponse;

@AutoConfiguration
@EnableConfigurationProperties(SecurityCookieProperties.class)
public class CookieAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SecurityCookieFactory securityCookieFactory(SecurityCookieProperties properties) {
        return new SecurityCookieFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HttpServletResponse.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    ServletAuthCookieWriter servletAuthCookieWriter(SecurityCookieFactory cookieFactory) {
        return new ServletAuthCookieWriter(cookieFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ServerHttpResponse.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    ReactiveCsrfCookieWriter reactiveCsrfCookieWriter(SecurityCookieFactory cookieFactory) {
        return new ReactiveCsrfCookieWriter(cookieFactory);
    }

}
