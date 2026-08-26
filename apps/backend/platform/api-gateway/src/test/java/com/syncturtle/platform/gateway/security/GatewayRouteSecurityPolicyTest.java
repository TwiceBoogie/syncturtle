package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;

@DisplayName("GatewayRouteSecurityPolicy")
class GatewayRouteSecurityPolicyTest {

    private final GatewayRouteSecurityPolicy policy = new GatewayRouteSecurityPolicy();

    @Nested
    @DisplayName("classify(HttpMethod, String)")
    class ClassifyTests {

        @Test
        @DisplayName("classifies final refresh and logout routes as public")
        void classifiesFinalRefreshAndLogoutRoutesAsPublic() {
            // arrange + act
            GatewayRouteSecurityCategory refresh = policy.classify(HttpMethod.POST, "/auth/refresh");
            GatewayRouteSecurityCategory normalLogout = policy.classify(HttpMethod.POST, "/auth/sign-out/");
            GatewayRouteSecurityCategory adminLogout = policy.classify(HttpMethod.POST, "/auth/admin/sign-out");
            // assert
            assertThat(refresh).isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(normalLogout).isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(adminLogout).isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            // verify
        }

        @Test
        @DisplayName("removes the old instance admin logout alias")
        void removesTheOldInstanceAdminLogoutAlias() {
            // arrange + act
            GatewayRouteSecurityCategory post = policy.classify(
                    HttpMethod.POST,
                    "/api/instances/admins/sign-out");
            GatewayRouteSecurityCategory options = policy.classify(
                    HttpMethod.OPTIONS,
                    "/api/instances/admins/sign-out");
            // assert
            assertThat(post).isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(options).isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            // verify
        }

        @Test
        @DisplayName("keeps admin completion exact and public")
        void keepsAdminCompletionExactAndPublic() {
            // arrange + act + assert
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session"))
                    .isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session/"))
                    .isEqualTo(GatewayRouteSecurityCategory.PUBLIC);
            assertThat(policy.classify(HttpMethod.POST, "/auth/admin/session"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.GET, "/auth/admin/session/extra"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            // verify
        }

        @Test
        @DisplayName("preserves options and protected routes")
        void preservesOptionalAndProtectedRoutes() {
            // arrange + act
            GatewayRouteSecurityCategory bootstrap = policy.classify(
                    HttpMethod.GET,
                    "/api/instances/admins/session");
            GatewayRouteSecurityCategory user = policy.classify(HttpMethod.GET, "/api/users/me");
            // assert
            assertThat(bootstrap).isEqualTo(GatewayRouteSecurityCategory.OPTIONAL_AUTH);
            assertThat(user).isEqualTo(GatewayRouteSecurityCategory.PROTECTED);
            // verify
        }

        @Test
        @DisplayName("defaults unknown and near match routes to deny")
        void defaultsUnknownAndNearMatchRoutesToDeny() {
            // arrange + act + assert
            assertThat(policy.classify(HttpMethod.POST, "/auth/admin/sign-out/extra"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.GET, "/auth/not-a-real-endpoint"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            assertThat(policy.classify(HttpMethod.OPTIONS, "/unclassified"))
                    .isEqualTo(GatewayRouteSecurityCategory.DEFAULT_DENY);
            // verify
        }

    }

}
