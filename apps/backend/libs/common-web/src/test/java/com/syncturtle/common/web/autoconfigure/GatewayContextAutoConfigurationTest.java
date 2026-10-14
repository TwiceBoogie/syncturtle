package com.syncturtle.common.web.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.filter.GatewayClientHeaderFilter;
import com.syncturtle.common.web.filter.GatewayUserHeaderFilter;

class GatewayContextAutoConfigurationTest {

    @Test
    void registersGatewayContextFiltersForServletApplications() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GatewayContextAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RequestUserContext.class);
                    assertThat(context).hasSingleBean(RequestClientContext.class);
                    FilterRegistrationBean<?> userRegistration = context.getBean(
                            "gatewayUserHeaderFilterRegistration", FilterRegistrationBean.class);
                    FilterRegistrationBean<?> clientRegistration = context.getBean(
                            "gatewayClientHeaderFilterRegistration", FilterRegistrationBean.class);
                    assertThat(userRegistration.getFilter()).isInstanceOf(GatewayUserHeaderFilter.class);
                    assertThat(clientRegistration.getFilter()).isInstanceOf(GatewayClientHeaderFilter.class);
                });
    }
}
