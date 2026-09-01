package com.syncturtle.platform.gateway.configuration.web;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.csrf.CsrfTokenSigner;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayCsrfProperties.class)
public class GatewayCsrfConfiguration {

    @Bean
    GatewayCsrfTokenProcessor gatewayCsrfTokenProcessor(
            JsonMapper jsonMapper,
            CsrfTokenSigner tokenSigner,
            SecureTokenGenerator tokenGenerator,
            GatewayCsrfProperties properties,
            Clock clock) {
        return new GatewayCsrfTokenProcessor(jsonMapper, tokenSigner, tokenGenerator, properties, clock);
    }

}
