package com.syncturtle.common.security.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HttpServletResponse.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(SecurityCookieConfiguration.class)
public class ServletAuthCookieConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ServletAuthCookieWriter servletAuthCookieWriter(SecurityCookieFactory cookieFactory) {
        return new ServletAuthCookieWriter(cookieFactory);
    }

}
