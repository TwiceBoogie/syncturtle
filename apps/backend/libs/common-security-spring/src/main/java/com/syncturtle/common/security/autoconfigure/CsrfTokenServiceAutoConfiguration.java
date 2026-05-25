package com.syncturtle.common.security.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.security.csrf.CsrfTokenSigner;
import com.syncturtle.common.security.csrf.impl.CsrfTokenServiceImpl;
import com.syncturtle.common.security.csrf.impl.HmacCsrfTokenSigner;
import com.syncturtle.common.security.properties.CsrfProperties;

@AutoConfiguration
@ConditionalOnClass(CsrfTokenSigner.class)
@EnableConfigurationProperties(CsrfProperties.class)
@ConditionalOnProperty(prefix = "app.security.csrf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CsrfTokenServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }

    @Bean
    @ConditionalOnMissingBean
    SecureTokenGenerator secureTokenGenerator() {
        return new Base64UrlSecureTokenGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(CsrfTokenSigner.class)
    CsrfTokenSigner csrfTokenSigner(CsrfProperties properties) {
        byte[] signingKey = HmacCsrfTokenSigner.decodeSigningKeyBase64Url(properties.getSigningKey());
        return new HmacCsrfTokenSigner(signingKey);
    }

    @Bean
    @ConditionalOnMissingBean
    CsrfTokenService csrfTokenService(
            SecureTokenGenerator tokenGenerator,
            CsrfTokenSigner tokenSigner,
            CsrfProperties properties) {
        return new CsrfTokenServiceImpl(tokenGenerator, tokenSigner, properties.getRawTokenBytes());
    }

}
