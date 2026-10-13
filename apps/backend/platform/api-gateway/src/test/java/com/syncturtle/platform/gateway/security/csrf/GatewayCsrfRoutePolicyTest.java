package com.syncturtle.platform.gateway.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.syncturtle.platform.gateway.type.GatewayCsrfRequirement;

public class GatewayCsrfRoutePolicyTest {

    private final GatewayCsrfRoutePolicy policy = new GatewayCsrfRoutePolicy();

    @Nested
    class RequirementTests {

        @Test
        void classifiesExactPreAuthRoutes() {
            assertThat(policy.requirement(HttpMethod.POST, "/auth/sign-in")).isEqualTo(GatewayCsrfRequirement.PREAUTH);
            assertThat(policy.requirement(HttpMethod.POST, "/auth/reset-password/uid/token"))
                    .isEqualTo(GatewayCsrfRequirement.PREAUTH);
            assertThat(policy.requirement(HttpMethod.POST, "/api/instances/admins/sign-up/"))
                    .isEqualTo(GatewayCsrfRequirement.PREAUTH);
        }

        @Test
        void classifiesTransportSessionRoutesWithoutBroadening() {
            assertThat(policy.requirement(HttpMethod.POST, "/auth/refresh"))
                    .isEqualTo(GatewayCsrfRequirement.TRANSPORT_SESSION);
            assertThat(policy.requirement(HttpMethod.POST, "/auth/sign-out"))
                    .isEqualTo(GatewayCsrfRequirement.TRANSPORT_SESSION);
            assertThat(policy.requirement(HttpMethod.POST, "/auth/admin/sign-out"))
                    .isEqualTo(GatewayCsrfRequirement.TRANSPORT_SESSION);
            assertThat(policy.requirement(HttpMethod.POST, "/auth/unknown")).isEqualTo(GatewayCsrfRequirement.NONE);
        }

        @Test
        void classifiesAuthenticatedSessionMutationsExactly() {
            assertThat(policy.requirement(HttpMethod.DELETE,
                    "/api/users/me/sessions/33333333-3333-3333-3333-333333333333"))
                    .isEqualTo(GatewayCsrfRequirement.AUTHENTICATED_SESSION);
            assertThat(policy.requirement(HttpMethod.POST, "/api/workspaces/acme/invitations"))
                    .isEqualTo(GatewayCsrfRequirement.AUTHENTICATED_SESSION);
            assertThat(policy.requirement(HttpMethod.PATCH, "/api/workspaces/acme/logo"))
                    .isEqualTo(GatewayCsrfRequirement.AUTHENTICATED_SESSION);
            assertThat(policy.requirement(HttpMethod.DELETE, "/api/assets/v1/asset-1"))
                    .isEqualTo(GatewayCsrfRequirement.AUTHENTICATED_SESSION);
            assertThat(policy.requirement(HttpMethod.DELETE, "/api/users/me/sessions/not-a-uuid"))
                    .isEqualTo(GatewayCsrfRequirement.NONE);
        }

    }

}
