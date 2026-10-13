package com.syncturtle.services.user.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.web.error.ApiErrorResponseFactory;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.configuration.property.FormCsrfFilterProperties;
import com.syncturtle.services.user.security.filter.FormCsrfOncePerRequestFilter;

public class UserFilterConfigurationTest {

    @Test
    void retainsDownstreamFormCsrfAfterRequestProvenanceOrder() {
        FormCsrfFilterProperties properties = new FormCsrfFilterProperties(true, null, null);
        FilterRegistrationBean<FormCsrfOncePerRequestFilter> registration = new UserFilterConfiguration()
                .formCsrfOncePerRequestFilterRegistration(
                        properties,
                        mock(CsrfTokenService.class),
                        mock(ServletAuthCookieWriter.class),
                        mock(PublicUrlResolver.class),
                        mock(ApiErrorResponseFactory.class));

        assertThat(registration.isEnabled()).isTrue();
        assertThat(registration.getOrder()).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getFilter()).isInstanceOf(FormCsrfOncePerRequestFilter.class);
    }

}
