package com.syncturtle.services.instance.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.provenance.RequestProvenancePolicy;

@Configuration(proxyBeanMethods = false)
public class InstanceRequestProvenanceConfiguration {

    @Bean
    RequestProvenancePolicy requestProvenancePolicy() {
        return RequestProvenancePolicy.instanceService();
    }

}
