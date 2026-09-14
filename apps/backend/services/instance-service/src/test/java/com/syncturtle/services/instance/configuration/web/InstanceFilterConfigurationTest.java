package com.syncturtle.services.instance.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.configuration.property.FormCsrfFilterProperties;
import com.syncturtle.services.instance.security.filter.FormCsrfOncePerRequestFilter;

class InstanceFilterConfigurationTest {

    @Test
    void retainsDownstreamFormCsrfAfterRequestProvenanceOrder() {
        FormCsrfFilterProperties properties = new FormCsrfFilterProperties(true, null, null);
        FilterRegistrationBean<FormCsrfOncePerRequestFilter> registration = new InstanceFilterConfiguration()
                .formCsrfOncePerRequestFilterRegistration(
                        properties,
                        mock(CsrfTokenService.class),
                        mock(ServletAuthCookieWriter.class),
                        mock(PublicUrlResolver.class));

        assertThat(registration.isEnabled()).isTrue();
        assertThat(registration.getOrder()).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getFilter()).isInstanceOf(FormCsrfOncePerRequestFilter.class);
    }

}
