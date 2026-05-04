package com.syncturtle.platform.gateway.configurations.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.spring.properties.CsrfCookiePolicyProperties;
import com.syncturtle.common.spring.properties.CsrfTransportProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ CsrfTransportProperties.class, CsrfCookiePolicyProperties.class,
        SessionCookieProperties.class,
        SessionGatewayProperties.class })
public class GatewaySecurityConfig {

}
