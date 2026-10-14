package com.syncturtle.services.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
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
        Import ownership = UserServiceApplication.class.getDeclaredAnnotation(Import.class);
        assertThat(ownership).isNotNull();
        Class<?>[] imports = ownership.value();

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PasswordHashingAutoConfiguration.class,
                        RequestProvenanceAutoConfiguration.class, ServletRequestProvenanceAutoConfiguration.class))
                .withUserConfiguration(imports)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityCookieProperties.class);
                    assertThat(context).hasSingleBean(SecurityCookieFactory.class);
                    assertThat(context).hasSingleBean(ServletAuthCookieWriter.class);
                });
    }

}
