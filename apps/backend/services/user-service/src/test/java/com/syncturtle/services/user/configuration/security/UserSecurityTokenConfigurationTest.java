package com.syncturtle.services.user.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.common.core.security.token.TokenHasher;

class UserSecurityTokenConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UserSecurityTokenConfiguration.class);

    @Test
    void ownsTheTokenPrimitivesRequiredBySessionAndPasswordFlows() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SecureTokenGenerator.class);
            assertThat(context).hasSingleBean(TokenHasher.class);
            assertThat(context.getBean(SecureTokenGenerator.class)).isInstanceOf(Base64UrlSecureTokenGenerator.class);
            assertThat(context.getBean(TokenHasher.class)).isInstanceOf(Sha256TokenHasher.class);
        });
    }

}
