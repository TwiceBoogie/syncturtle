package com.syncturtle.platform.gateway.configuration.web;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfSigningProperties;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfCookieWriter;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenSigner;
import com.syncturtle.platform.gateway.security.csrf.HmacGatewayCsrfTokenSigner;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ GatewayCsrfProperties.class, GatewayCsrfSigningProperties.class })
public class GatewayCsrfConfiguration {

    @Bean
    GatewayCsrfTokenSigner gatewayCsrfTokenSigner(GatewayCsrfSigningProperties signingProperties) {
        byte[] signingKey = HmacGatewayCsrfTokenSigner.decodeSigningKeyBase64Url(signingProperties.getKey());
        return new HmacGatewayCsrfTokenSigner(signingKey);
    }

    @Bean
    SecureTokenGenerator gatewayCsrfTokenGenerator() {
        return new Base64UrlSecureTokenGenerator();
    }

    @Bean
    GatewayCsrfCookieWriter gatewayCsrfCookieWriter(SecurityCookieFactory cookieFactory) {
        return new GatewayCsrfCookieWriter(cookieFactory);
    }

    @Bean
    GatewayCsrfTokenProcessor gatewayCsrfTokenProcessor(
            JsonMapper jsonMapper,
            GatewayCsrfTokenSigner tokenSigner,
            SecureTokenGenerator tokenGenerator,
            GatewayCsrfProperties properties,
            Clock clock) {
        return new GatewayCsrfTokenProcessor(jsonMapper, tokenSigner, tokenGenerator, properties, clock);
    }

}
