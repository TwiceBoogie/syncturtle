package com.syncturtle.platform.gateway.configurations.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.platform.gateway.configurations.properties.SessionCookieProperties;
import com.syncturtle.platform.gateway.configurations.properties.SessionGatewayProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ SessionCookieProperties.class, SessionGatewayProperties.class })
public class RedisSessionConfig {

}
