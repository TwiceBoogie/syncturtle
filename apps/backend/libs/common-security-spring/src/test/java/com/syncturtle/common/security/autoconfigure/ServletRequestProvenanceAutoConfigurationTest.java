package com.syncturtle.common.security.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceOncePerRequestFilter;
import com.syncturtle.common.security.provenance.RequestProvenanceRoutePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceVerifier;

class ServletRequestProvenanceAutoConfigurationTest {

    @Test
    void registersVerifierAtAbsoluteFirstServletOrder() {
        ServletRequestProvenanceAutoConfiguration configuration = new ServletRequestProvenanceAutoConfiguration();

        FilterRegistrationBean<RequestProvenanceOncePerRequestFilter> registration = configuration
                .requestProvenanceFilterRegistration(
                        mock(RequestProvenanceVerifier.class),
                        mock(RequestProvenanceRoutePolicy.class),
                        new RequestProvenanceCanonicalizer());

        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
        assertThat(registration.getFilterName()).isEqualTo("requestProvenanceOncePerRequestFilter");
    }

}
