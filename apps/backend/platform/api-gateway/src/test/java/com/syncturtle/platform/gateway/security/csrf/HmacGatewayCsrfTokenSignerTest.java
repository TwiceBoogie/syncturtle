package com.syncturtle.platform.gateway.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HmacGatewayCsrfTokenSignerTest {

    private static final byte[] KEY = "k".repeat(32).getBytes(StandardCharsets.UTF_8);

    @Nested
    class SignAndVerifyTests {

        @Test
        void signsVerifiesAndExtractsThePayload() {
            HmacGatewayCsrfTokenSigner signer = new HmacGatewayCsrfTokenSigner(KEY);

            String signed = signer.sign("payload");

            assertThat(signer.verify(signed)).isTrue();
            assertThat(signer.extractToken(signed)).isEqualTo("payload");
            assertThat(signer.verify(signed + "tampered")).isFalse();
        }

        @Test
        void decodesConfiguredKeyAndRejectsWeakOrInvalidKeyMaterial() {
            String canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(KEY);

            assertThat(HmacGatewayCsrfTokenSigner.decodeSigningKeyBase64Url(canonical)).isEqualTo(KEY);
            assertThatThrownBy(() -> new HmacGatewayCsrfTokenSigner(new byte[31]))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> HmacGatewayCsrfTokenSigner.decodeSigningKeyBase64Url("not valid base64url!"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

}
