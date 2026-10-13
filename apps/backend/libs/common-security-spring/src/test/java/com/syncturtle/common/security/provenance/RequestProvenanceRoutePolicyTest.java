package com.syncturtle.common.security.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;

class RequestProvenanceRoutePolicyTest {

    private static final RequestHeaderSource NO_HEADERS = name -> List.of();

    @Test
    void allowsActorOnlyOnTheTwoCodeOwnedInstanceToWorkspaceOperations() {
        // arrange
        RequestProvenanceRoutePolicy policy = workspacePolicy();
        RequestProvenanceClaims claims = claims("instance-service", "workspace-service");
        RequestHeaderSource actor = name -> name.equalsIgnoreCase(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID)
                ? List.of("user-1")
                : List.of();
        // conditions
        // act + assert
        assertThatCode(() -> policy.authorize(
                claims, "GET", "/internal/v1/instance-workspaces/slug-check", actor))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.authorize(
                claims, "POST", "/internal/v1/instance-workspaces", actor))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.authorize(
                claims, "GET", "/api/workspaces/current", actor))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize(
                claims, "POST", "/internal/v1/instance-workspaces/other", actor))
                .isInstanceOf(RequestProvenanceException.class);
        // verify
    }

    @Test
    void rejectsWrongIssuerMissingActorAndInternalUseOfGatewayOnlyHeaders() {
        // assert
        RequestProvenanceRoutePolicy policy = workspacePolicy();
        RequestHeaderSource forgedIdentity = name -> {
            if (name.equalsIgnoreCase(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID)) {
                return List.of("actor-user");
            }
            if (name.equalsIgnoreCase(GatewayHeaders.HDR_AUTH_USER_ID)) {
                return List.of("forged-user");
            }
            return List.of();
        };
        // conditions
        // act + assert
        assertThatThrownBy(() -> policy.authorize(
                claims("api-gateway", "workspace-service"),
                "GET", "/internal/v1/instance-workspaces/slug-check", NO_HEADERS))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize(
                claims("instance-service", "workspace-service"),
                "GET", "/internal/v1/instance-workspaces/slug-check", NO_HEADERS))
                .isInstanceOf(RequestProvenanceException.class);
        assertThatThrownBy(() -> policy.authorize(
                claims("instance-service", "workspace-service"),
                "GET", "/internal/v1/instance-workspaces/slug-check", forgedIdentity))
                .isInstanceOf(RequestProvenanceException.class);
        // verify
    }

    @Test
    void keepsOperationalExemptionsAndProtectedRoutesInCode() {
        // arrange
        RequestProvenanceRoutePolicy userPolicy = new RequestProvenanceRoutePolicy(
                RequestProvenancePolicy.userService(),
                new RequestProvenanceCanonicalizer());
        // conditions
        // act + assert
        assertThat(userPolicy.isExempt("GET", "/actuator/health")).isTrue();
        assertThat(userPolicy.isExempt("GET", "/actuator/health/readiness")).isTrue();
        assertThat(userPolicy.isExempt("GET", "/.well-known/jwks.json")).isTrue();
        assertThat(userPolicy.isExempt("GET", "/ping")).isFalse();
        assertThat(userPolicy.isExempt("GET", "/api/users/me")).isFalse();
        // verify
    }

    @Test
    void knownIssuerKeyCannotGrantPermissionOutsideItsCodeOwnedRoutes() {
        // arrange
        RequestProvenanceRoutePolicy userPolicy = new RequestProvenanceRoutePolicy(
                RequestProvenancePolicy.userService(),
                new RequestProvenanceCanonicalizer());
        // conditions
        // act + assert
        assertThatThrownBy(() -> userPolicy.authorize(
                claims("instance-service", "user-service"),
                "GET",
                "/api/users/me",
                NO_HEADERS))
                .isInstanceOf(RequestProvenanceException.class);
        // verify
    }

    private static RequestProvenanceRoutePolicy workspacePolicy() {
        return new RequestProvenanceRoutePolicy(
                RequestProvenancePolicy.workspaceService(),
                new RequestProvenanceCanonicalizer());
    }

    private static RequestProvenanceClaims claims(String issuer, String audience) {
        return RequestProvenanceClaims.issue(
                "A".repeat(22),
                issuer,
                audience,
                Instant.parse("2026-09-03T12:00:00Z"),
                "GET",
                "/",
                "A".repeat(43),
                "B".repeat(43));
    }

}
