package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;

class CsrfIssuanceRoutePolicyTest {
    private final GatewayRouteSecurityPolicy policy = new GatewayRouteSecurityPolicy();

    @Nested
    class Classify {

        @Test
        void csrfIssuanceUsesOptionalAuthentication() {
            assertThat(policy.classify(HttpMethod.GET, "/api/get-csrf-token"))
                    .isEqualTo(GatewayRouteSecurityCategory.OPTIONAL_AUTH);
            assertThat(policy.classify(HttpMethod.HEAD, "/api/get-csrf-token"))
                    .isEqualTo(GatewayRouteSecurityCategory.OPTIONAL_AUTH);
        }

        @Test
        void nearMatchesRemainDefaultDeny() {
            assertThat(policy.classify(HttpMethod.GET, "/api/get-csrf-token/extra"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.POST, "/api/get-csrf-token"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
        }

    }
}
