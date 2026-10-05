package com.syncturtle.services.email.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.provenance.RequestProvenancePolicy;

@Configuration(proxyBeanMethods = false)
public class EmailRequestProvenanceConfiguration {

    @Bean
    RequestProvenancePolicy requestProvenancePolicy() {
        return RequestProvenancePolicy.emailService();
    }

}
