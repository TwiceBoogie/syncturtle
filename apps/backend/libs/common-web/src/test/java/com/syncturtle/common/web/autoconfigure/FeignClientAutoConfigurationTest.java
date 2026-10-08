package com.syncturtle.common.web.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import feign.Request;

class FeignClientAutoConfigurationTest {
    @Test
    void internalClientsReceiveTheSharedTimeoutPolicyWithoutClientSpecificProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FeignClientAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Request.Options options = context.getBean(Request.Options.class);
                    assertThat(options.connectTimeoutMillis()).isEqualTo(2_000);
                    assertThat(options.readTimeoutMillis()).isEqualTo(5_000);
                    assertThat(options.isFollowRedirects()).isTrue();
                });
    }
}
