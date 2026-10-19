package com.syncturtle.platform.gateway.configuration.web;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.platform.gateway.security.browser.GatewayBrowserOriginPolicy;

@Configuration(proxyBeanMethods = false)
public class GatewayBrowserRequestConfiguration {

    @Bean
    GatewayBrowserOriginPolicy gatewayBrowserOriginPolicy(GlobalCorsProperties properties) {
        return new GatewayBrowserOriginPolicy(properties);
    }

    @Bean
    SecureTokenGenerator browserBindingTokenGenerator() {
        return new Base64UrlSecureTokenGenerator();
    }

}
