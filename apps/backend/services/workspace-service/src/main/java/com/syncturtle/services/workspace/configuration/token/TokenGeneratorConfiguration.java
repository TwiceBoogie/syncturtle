package com.syncturtle.services.workspace.configuration.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;

@Configuration(proxyBeanMethods = false)
public class TokenGeneratorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SecureTokenGenerator secureTokenGenerator() {
        return new Base64UrlSecureTokenGenerator();
    }

}
