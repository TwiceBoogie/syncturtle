package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.spring.properties.CsrfProperties;
import com.syncturtle.common.web.security.csrf.CsrfTokenSigner;
import com.syncturtle.common.web.security.csrf.HmacCsrfTokenSigner;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CsrfProperties.class)
public class HmacCsrfTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CsrfTokenSigner csrfTokenSigner(CsrfProperties props) {
        byte[] key = HmacCsrfTokenSigner.decodeSigningKeyBase64Url(props.getSigningKey());
        return new HmacCsrfTokenSigner(props.getHmacAlgorithm(), key);
    }

}
