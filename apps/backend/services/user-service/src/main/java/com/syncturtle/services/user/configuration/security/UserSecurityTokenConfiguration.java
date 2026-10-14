package com.syncturtle.services.user.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.common.core.security.token.TokenHasher;

@Configuration(proxyBeanMethods = false)
public class UserSecurityTokenConfiguration {

    @Bean
    SecureTokenGenerator secureTokenGenerator() {
        return new Base64UrlSecureTokenGenerator();
    }

    @Bean
    TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }

}
