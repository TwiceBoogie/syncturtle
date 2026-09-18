package com.syncturtle.platform.gateway.configuration.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.provenance.RequestProvenancePolicy;
import com.syncturtle.platform.gateway.security.provenance.GatewayRequestProvenancePolicy;

@Configuration(proxyBeanMethods = false)
public class GatewayRequestProvenanceConfiguration {

    @Bean
    RequestProvenancePolicy requestProvenancePolicy() {
        return RequestProvenancePolicy.apiGateway();
    }

    @Bean
    GatewayRequestProvenancePolicy gatewayRequestProvenancePolicy() {
        return new GatewayRequestProvenancePolicy();
    }

}
