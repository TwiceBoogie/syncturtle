package com.syncturtle.services.workspace.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.security.provenance.RequestProvenancePolicy;

@Configuration(proxyBeanMethods = false)
public class WorkspaceRequestProvenanceConfiguration {

    @Bean
    RequestProvenancePolicy requestProvenancePolicy() {
        return RequestProvenancePolicy.workspaceService();
    }

}
