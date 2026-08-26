package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;

class AdminSessionCompletionRoutePolicyTest {

    @Nested
    class ClassifyTests {

        @Test
        @DisplayName("permits only the exact get completion path")
        void permitsOnlyTheExactGetCompletionPath() {
            // arrange
            GatewayRouteSecurityPolicy policy = new GatewayRouteSecurityPolicy();
            // conditions
            // act + assert
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session"))
                    .isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session/"))
                    .isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(policy.classify(HttpMethod.POST, "/auth/admin/session"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.HEAD, "/auth/admin/session"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session/extra"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
        }

    }

}
