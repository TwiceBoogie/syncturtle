package com.syncturtle.platform.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Import;

import com.syncturtle.common.security.autoconfigure.PasswordHashingAutoConfiguration;
import com.syncturtle.common.security.autoconfigure.RequestProvenanceAutoConfiguration;
import com.syncturtle.common.security.autoconfigure.ServletRequestProvenanceAutoConfiguration;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.security.property.SecurityCookieProperties;

class CookieApplicationOwnershipTest {

    @Test
    void applicationImportsProvideOnlyOwnedCookieCapabilities() {
        Import ownership = ApiGatewayApplication.class.getDeclaredAnnotation(Import.class);
        assertThat(ownership).isNotNull();
        Class<?>[] imports = ownership.value();

        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PasswordHashingAutoConfiguration.class,
                        RequestProvenanceAutoConfiguration.class, ServletRequestProvenanceAutoConfiguration.class))
                .withUserConfiguration(imports)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityCookieProperties.class);
                    assertThat(context).hasSingleBean(SecurityCookieFactory.class);
                    assertThat(context).doesNotHaveBean(ServletAuthCookieWriter.class);
                });
    }

}
