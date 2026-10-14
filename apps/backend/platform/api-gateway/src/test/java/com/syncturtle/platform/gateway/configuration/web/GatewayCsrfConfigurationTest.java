package com.syncturtle.platform.gateway.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfSigningProperties;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfCookieWriter;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenSigner;
import com.syncturtle.platform.gateway.security.csrf.HmacGatewayCsrfTokenSigner;

import tools.jackson.databind.json.JsonMapper;

class GatewayCsrfConfigurationTest {

    private static final byte[] key = "k".repeat(32).getBytes(StandardCharsets.UTF_8);

    @Test
    void ownsAllGatewayCsrfImplementationBeans() {
        GatewayCsrfConfiguration configuration = new GatewayCsrfConfiguration();
        String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(key);
        GatewayCsrfTokenSigner signer = configuration
                .gatewayCsrfTokenSigner(new GatewayCsrfSigningProperties(encodedKey));
        SecureTokenGenerator tokenGenerator = configuration.gatewayCsrfTokenGenerator();
        SecurityCookieFactory cookieFactory = new SecurityCookieFactory(new SecurityCookieProperties(
                false, false, "Lax", "Lax", "Lax"));

        GatewayCsrfCookieWriter cookieWriter = configuration.gatewayCsrfCookieWriter(cookieFactory);
        GatewayCsrfTokenProcessor processor = configuration.gatewayCsrfTokenProcessor(
                new JsonMapper(),
                signer,
                tokenGenerator,
                Clock.systemUTC());

        assertThat(signer).isInstanceOf(HmacGatewayCsrfTokenSigner.class);
        assertThat(cookieWriter).isNotNull();
        assertThat(processor).isNotNull();
    }

}
