package com.syncturtle.common.security.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.type.ActorHeaderMode;

class RequestProvenanceOutboundPolicyTest {

    private final RequestProvenanceOutboundPolicy policy = new RequestProvenanceOutboundPolicy(
            RequestProvenancePolicy.instanceService(), new RequestProvenanceCanonicalizer());

    @Test
    void derivesClientAndAudienceFromTheDestinationWorkload() {
        RequestProvenanceOutboundPolicy.AuthorizedOutboundRoute route = policy.authorize(
                "user-service",
                "POST",
                "/internal/v1/users/admins/sign-in");

        assertThat(route.getAudience()).isEqualTo("user-service");
        assertThat(route.getActorMode()).isEqualTo(ActorHeaderMode.FORBIDDEN);
    }

    @Test
    void requiresActorOnlyForTheTwoInstanceWorkspaceOperations() {
        RequestProvenanceOutboundPolicy.AuthorizedOutboundRoute slug = policy.authorize(
                "workspace-service",
                "GET",
                "/internal/v1/instance-workspaces/slug-check");
        RequestProvenanceOutboundPolicy.AuthorizedOutboundRoute create = policy.authorize(
                "workspace-service",
                "POST",
                "/internal/v1/instance-workspaces");

        assertThat(slug.getActorMode()).isEqualTo(ActorHeaderMode.REQUIRED);
        assertThat(create.getActorMode()).isEqualTo(ActorHeaderMode.REQUIRED);
    }

    @Test
    void rejectsWrongClientMethodPathAndMalformedPath() {
        assertThatThrownBy(() -> policy.authorize("oauth-provider", "POST", "/internal/v1/users/admins/sign-in"))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize("user-service", "GET", "/internal/v1/users/admins/sign-in"))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize("user-service", "POST", "/internal/v1/users/admins/unknown"))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize("user-service", "POST", "/internal/%ZZ"))
                .isInstanceOf(RequestProvenanceException.class);
    }

}
