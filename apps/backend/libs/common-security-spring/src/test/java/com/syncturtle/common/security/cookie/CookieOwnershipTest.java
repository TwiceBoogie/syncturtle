package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import com.syncturtle.common.security.configuration.SecurityCookieConfiguration;
import com.syncturtle.common.security.configuration.ServletAuthCookieConfiguration;
import com.syncturtle.common.security.property.SecurityCookieProperties;

class CookieOwnershipTest {

    @Test
    void sharedLibraryDoesNotAutomaticallyOptEveryServiceIntoCookies() {
        ImportCandidates candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader());
        assertThat(candidates.getCandidates())
                .doesNotContain(SecurityCookieConfiguration.class.getName(),
                        ServletAuthCookieConfiguration.class.getName(),
                        "com.syncturtle.common.security.autoconfigure.CookieAutoConfiguration");
    }

    @Test
    void anExplicitServletOwnerReceivesTheFactoryAndWriterOnce() {
        new WebApplicationContextRunner()
                .withUserConfiguration(SecurityCookieConfiguration.class, ServletAuthCookieConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityCookieProperties.class);
                    assertThat(context).hasSingleBean(SecurityCookieFactory.class);
                    assertThat(context).hasSingleBean(ServletAuthCookieWriter.class);
                });
    }

    @Test
    void factoryOwnershipDoesNotImplyServletWriterOwnership() {
        new WebApplicationContextRunner()
                .withUserConfiguration(SecurityCookieConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityCookieFactory.class);
                    assertThat(context).doesNotHaveBean(ServletAuthCookieWriter.class);
                });
    }

    @Test
    void aNonWebProcessDoesNotReceiveCookiePropertiesOrBeans() {
        new ApplicationContextRunner()
                .withUserConfiguration(ServletAuthCookieConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecurityCookieProperties.class);
                    assertThat(context).doesNotHaveBean(SecurityCookieFactory.class);
                    assertThat(context).doesNotHaveBean(ServletAuthCookieWriter.class);
                });
    }

}
