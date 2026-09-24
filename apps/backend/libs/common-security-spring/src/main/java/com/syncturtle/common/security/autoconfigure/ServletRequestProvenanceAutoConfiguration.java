package com.syncturtle.common.security.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceOncePerRequestFilter;
import com.syncturtle.common.security.provenance.RequestProvenanceRoutePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceVerifier;

import jakarta.servlet.DispatcherType;

@AutoConfiguration(after = RequestProvenanceAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(RequestProvenanceVerifier.class)
public class ServletRequestProvenanceAutoConfiguration {

    @Bean
    FilterRegistrationBean<RequestProvenanceOncePerRequestFilter> requestProvenanceFilterRegistration(
            RequestProvenanceVerifier verifier,
            RequestProvenanceRoutePolicy routePolicy,
            RequestProvenanceCanonicalizer canonicalizer) {
        RequestProvenanceOncePerRequestFilter filter = new RequestProvenanceOncePerRequestFilter(verifier, routePolicy,
                canonicalizer);
        FilterRegistrationBean<RequestProvenanceOncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setName("requestProvenanceOncePerRequestFilter");
        registration.setFilter(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
