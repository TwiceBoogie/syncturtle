package com.syncturtle.services.file;

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
        Import ownership = FileServiceApplication.class.getDeclaredAnnotation(Import.class);
        assertThat(ownership).isNull();
        Class<?>[] imports = new Class<?>[0];

        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PasswordHashingAutoConfiguration.class,
                        RequestProvenanceAutoConfiguration.class, ServletRequestProvenanceAutoConfiguration.class))
                .withUserConfiguration(imports)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecurityCookieProperties.class);
                    assertThat(context).doesNotHaveBean(SecurityCookieFactory.class);
                    assertThat(context).doesNotHaveBean(ServletAuthCookieWriter.class);
                });
    }

}
