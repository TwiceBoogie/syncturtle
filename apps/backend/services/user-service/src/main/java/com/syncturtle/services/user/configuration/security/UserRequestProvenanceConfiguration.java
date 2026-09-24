package com.syncturtle.services.user.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.provenance.RequestProvenancePolicy;

@Configuration(proxyBeanMethods = false)
public class UserRequestProvenanceConfiguration {

    @Bean
    RequestProvenancePolicy requestProvenancePolicy() {
        return RequestProvenancePolicy.userService();
    }

}
