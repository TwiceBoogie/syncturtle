package com.syncturtle.common.web.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceOutboundPolicy;
import com.syncturtle.common.security.provenance.RequestProvenancePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;

import feign.RequestTemplate;
import feign.Request.HttpMethod;
import feign.Target.HardCodedTarget;

class RequestProvenanceFeignInterceptorTest {

    private RequestProvenanceSigner signer;
    private RequestProvenanceFeignInterceptor interceptor;

    @BeforeEach
    void setup() {
        signer = mock(RequestProvenanceSigner.class);
        RequestProvenanceCanonicalizer canonicalizer = new RequestProvenanceCanonicalizer();
        RequestProvenanceOutboundPolicy policy = new RequestProvenanceOutboundPolicy(
                RequestProvenancePolicy.instanceService(), canonicalizer);
        interceptor = new RequestProvenanceFeignInterceptor(signer, policy, canonicalizer);
    }

    @Nested
    class ApplyTests {

        @Test
        void signsAllowedCallFreshAndDoesNotForwardProtectedHeaders() {
            // arrange
            RequestTemplate first = template("user-service", "/internal/v1/users", HttpMethod.POST, "/admins/sign-in");
            first.header(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, "browser-assertion");
            first.header(GatewayHeaders.HDR_AUTH_USER_ID, "browser-user");
            RequestTemplate second = template("user-service", "/internal/v1/users", HttpMethod.POST, "/admins/sign-in");
            // conditions
            when(signer.sign(eq("user-service"), eq("POST"), eq("/internal/v1/users/admins/sign-in"), eq(null), any()))
                    .thenReturn("fresh-1", "fresh-2");
            // act
            interceptor.apply(first);
            interceptor.apply(second);
            // assert
            assertThat(first.headers().get(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE)).containsExactly("fresh-1");
            assertThat(second.headers().get(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE)).containsExactly("fresh-2");
            assertThat(first.headers()).doesNotContainKey(GatewayHeaders.HDR_AUTH_USER_ID);
        }

        @Test
        void rejectsWrongRouteAndDoesNotSignExternalClientTraffic() {
            // arrange
            RequestTemplate wrongRoute = template("user-service", "/internal/v1/users", HttpMethod.GET,
                    "/admins/sign-in");
            RequestTemplate external = template("oauth-provider", "", HttpMethod.POST, "/oauth/token");
            // conditions
            // act + assert
            assertThatThrownBy(() -> interceptor.apply(wrongRoute))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> interceptor.apply(external))
                    .isInstanceOf(RequestProvenanceException.class);
            // verify
            verify(signer, never()).sign(any(), any(), any(), any(), any());
        }

        @Test
        void preservesOneExplicitActorOnlyForApprovedActorRoutes() {
            when(signer.sign(eq("workspace-service"), any(), any(), any(), any()))
                    .thenReturn("slug-assertion", "create-assertion");

            RequestTemplate slug = template(
                    "workspace-service", "/internal/v1/instance-workspaces", HttpMethod.GET, "/slug-check?slug=demo");
            slug.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "actor-user");
            RequestTemplate create = template(
                    "workspace-service", "/internal/v1/instance-workspaces", HttpMethod.POST, "");
            create.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "actor-user");

            interceptor.apply(slug);
            interceptor.apply(create);

            assertThat(slug.headers().get(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID))
                    .containsExactly("actor-user");
            assertThat(create.headers().get(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID))
                    .containsExactly("actor-user");
            verify(signer).sign(eq("workspace-service"), eq("GET"),
                    eq("/internal/v1/instance-workspaces/slug-check"), eq("slug=demo"), any());
            verify(signer).sign(eq("workspace-service"), eq("POST"),
                    eq("/internal/v1/instance-workspaces"), eq(null), any());
        }

        @Test
        void rejectsMissingDuplicateOrNonCanonicalActor() {
            RequestTemplate missing = template(
                    "workspace-service", "/internal/v1/instance-workspaces", HttpMethod.GET, "/slug-check");
            RequestTemplate duplicate = template(
                    "workspace-service", "/internal/v1/instance-workspaces", HttpMethod.GET, "/slug-check");
            duplicate.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "actor-one", "actor-two");
            RequestTemplate nonCanonical = template(
                    "workspace-service", "/internal/v1/instance-workspaces", HttpMethod.GET, "/slug-check");
            nonCanonical.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, " actor-user ");

            assertThatThrownBy(() -> interceptor.apply(missing))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> interceptor.apply(duplicate))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> interceptor.apply(nonCanonical))
                    .isInstanceOf(RequestProvenanceException.class);
            verify(signer, never()).sign(any(), any(), any(), any(), any());
        }

        @Test
        void rejectsExplicitActorOnForbiddenRoute() {
            RequestTemplate forbidden = template(
                    "user-service", "/internal/v1/users", HttpMethod.POST, "/admins/sign-in");
            forbidden.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "actor-user");

            assertThatThrownBy(() -> interceptor.apply(forbidden))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("forbids actors");
            verify(signer, never()).sign(any(), any(), any(), any(), any());
        }

    }

    private static RequestTemplate template(String targetName, String basePath, HttpMethod method, String url) {
        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new HardCodedTarget<>(Object.class, targetName, "http://localhost" + basePath));
        template.method(method);
        template.uri(url);
        return template;
    }

}
