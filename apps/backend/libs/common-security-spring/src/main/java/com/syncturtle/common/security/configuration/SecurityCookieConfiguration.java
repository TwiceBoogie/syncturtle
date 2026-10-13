package com.syncturtle.common.security.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableConfigurationProperties(SecurityCookieProperties.class)
public class SecurityCookieConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SecurityCookieFactory securityCookieFactory(SecurityCookieProperties properties) {
        return new SecurityCookieFactory(properties);
    }

}
