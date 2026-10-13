package com.syncturtle.common.security.provenance;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.DELETE;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import com.syncturtle.common.security.type.ActorHeaderMode;

import lombok.Getter;

@Getter
public final class RequestProvenancePolicy {

    private static final Set<HttpMethod> APPLICATION_METHODS = Set.of(GET, HEAD, OPTIONS, POST, PUT, PATCH, DELETE);
    private static final List<ExemptRoute> HEALTH_EXEMPTIONS = List.of(exemption(GET, "/actuator/health"),
            exemption(GET, "/actuator/health/**"));

    private final Workload workload;
    private final List<InboundRoute> inboundRoutes;
    private final List<ExemptRoute> exemptRoutes;
    private final List<OutboundRoute> outboundRoutes;
    private final Set<String> trustedIssuerIds;

    private RequestProvenancePolicy(
            Workload workload,
            List<InboundRoute> inboundRoutes,
            List<ExemptRoute> exemptRoutes,
            List<OutboundRoute> outboundRoutes) {
        Assert.notNull(workload, "workload is required");
        Assert.notNull(inboundRoutes, "inboundRoutes is required");
        Assert.notNull(exemptRoutes, "exemptRoutes is required");
        Assert.notNull(outboundRoutes, "outboundRoutes is required");
        Assert.state(workload.signingMode != SigningMode.VERIFIER_ONLY || outboundRoutes.isEmpty(),
                "verifier-only workload cannot define outbound routes");

        this.workload = workload;
        this.inboundRoutes = List.copyOf(inboundRoutes);
        this.exemptRoutes = List.copyOf(exemptRoutes);
        this.outboundRoutes = List.copyOf(outboundRoutes);
        this.trustedIssuerIds = collectTrustedIssuerIds(inboundRoutes);
    }

    public static RequestProvenancePolicy apiGateway() {
        return policy(Workload.API_GATEWAY, List.of(), List.of(), List.of());
    }

    public static RequestProvenancePolicy instanceService() {
        List<InboundRoute> inboundRoutes = List.of(gatewayRoute("/api/**"),
                internalRoute(GET, "/internal/v1/instances/configurations/user-auth-config", Workload.USER_SERVICE),
                internalRoute(GET, "/internal/v1/instances/configurations/user-auth-config-secrets",
                        Workload.USER_SERVICE),
                internalRoute(POST, "/internal/v1/instances/authz-view", Workload.USER_SERVICE),
                internalRoute(GET, "/internal/v1/instances/configurations/workspace-config",
                        Workload.WORKSPACE_SERVICE),
                internalRoute(GET, "/internal/v1/instances/configurations/email-config-secrets",
                        Workload.EMAIL_SERVICE));
        List<OutboundRoute> outboundRoutes = List.of(
                outbound(POST, "/internal/v1/users/admins/sign-up", Workload.USER_SERVICE),
                outbound(POST, "/internal/v1/users/admins/sign-in", Workload.USER_SERVICE),
                outbound(POST, "/internal/v1/users/admins/session-handoffs", Workload.USER_SERVICE),
                actorOutbound(GET, "/internal/v1/instance-workspaces/slug-check", Workload.WORKSPACE_SERVICE),
                actorOutbound(POST, "/internal/v1/instance-workspaces", Workload.WORKSPACE_SERVICE));

        return policy(Workload.INSTANCE_SERVICE, inboundRoutes, HEALTH_EXEMPTIONS, outboundRoutes);
    }

    public static RequestProvenancePolicy userService() {
        List<InboundRoute> inboundRoutes = List.of(
                gatewayRoute("/auth/**"),
                gatewayRoute("/api/**"),
                internalRoute(POST, "/internal/v1/users/admins/sign-up", Workload.INSTANCE_SERVICE),
                internalRoute(POST, "/internal/v1/users/admins/sign-in", Workload.INSTANCE_SERVICE),
                internalRoute(POST, "/internal/v1/users/admins/session-handoffs", Workload.INSTANCE_SERVICE));
        List<ExemptRoute> exemptRoutes = List.of(
                exemption(GET, "/actuator/health"),
                exemption(GET, "/actuator/health/**"),
                exemption(GET, "/.well-known/jwks.json"));
        List<OutboundRoute> outboundRoutes = List.of(
                outbound(GET, "/internal/v1/instances/configurations/user-auth-config", Workload.INSTANCE_SERVICE),
                outbound(GET, "/internal/v1/instances/configurations/user-auth-config-secrets",
                        Workload.INSTANCE_SERVICE),
                outbound(POST, "/internal/v1/instances/authz-view", Workload.INSTANCE_SERVICE),
                outbound(GET, "/internal/v1/assets/{assetId}/validation", Workload.FILE_SERVICE));

        return policy(Workload.USER_SERVICE, inboundRoutes, exemptRoutes, outboundRoutes);
    }

    public static RequestProvenancePolicy workspaceService() {
        List<InboundRoute> inboundRoutes = List.of(
                gatewayRoute("/api/**"),
                actorInbound(GET, "/internal/v1/instance-workspaces/slug-check", Workload.INSTANCE_SERVICE),
                actorInbound(POST, "/internal/v1/instance-workspaces", Workload.INSTANCE_SERVICE));
        List<OutboundRoute> outboundRoutes = List.of(
                outbound(GET, "/internal/v1/instances/configurations/workspace-config", Workload.INSTANCE_SERVICE),
                outbound(GET, "/internal/v1/assets/{assetId}/validation", Workload.FILE_SERVICE));

        return policy(Workload.WORKSPACE_SERVICE, inboundRoutes, HEALTH_EXEMPTIONS, outboundRoutes);
    }

    public static RequestProvenancePolicy fileService() {
        List<InboundRoute> inboundRoutes = List.of(
                gatewayRoute("/api/**"),
                internalRoute(GET, "/internal/v1/assets/{assetId}/validation", Workload.USER_SERVICE,
                        Workload.WORKSPACE_SERVICE));

        return policy(Workload.FILE_SERVICE, inboundRoutes, HEALTH_EXEMPTIONS, List.of());
    }

    public static RequestProvenancePolicy emailService() {
        return policy(
                Workload.EMAIL_SERVICE,
                List.of(gatewayRoute("/api/**")),
                HEALTH_EXEMPTIONS,
                List.of(outbound(GET, "/internal/v1/instances/configurations/email-config-secrets",
                        Workload.INSTANCE_SERVICE)));
    }

    public String getWorkloadId() {
        return workload.id;
    }

    public boolean isSigningRequired() {
        return workload.signingMode == SigningMode.SIGNER;
    }

    private static RequestProvenancePolicy policy(
            Workload workload,
            List<InboundRoute> inboundRoutes,
            List<ExemptRoute> exemptRoutes,
            List<OutboundRoute> outboundRoutes) {
        return new RequestProvenancePolicy(
                workload,
                inboundRoutes,
                exemptRoutes,
                outboundRoutes);
    }

    private static Set<String> collectTrustedIssuerIds(List<InboundRoute> routes) {
        Set<String> issuers = new LinkedHashSet<>();
        for (InboundRoute route : routes) {
            issuers.addAll(route.issuerIds);
        }

        return Set.copyOf(issuers);
    }

    private static InboundRoute gatewayRoute(String pathPattern) {
        return new InboundRoute(
                APPLICATION_METHODS,
                pathPattern,
                Set.of(Workload.API_GATEWAY.id),
                ActorHeaderMode.FORBIDDEN,
                TrustedHeaderMode.ALLOWED);
    }

    private static InboundRoute internalRoute(HttpMethod method, String pathPattern, Workload... issuers) {
        return new InboundRoute(
                Set.of(method),
                pathPattern,
                workloadIds(issuers),
                ActorHeaderMode.FORBIDDEN,
                TrustedHeaderMode.FORBIDDEN);
    }

    private static InboundRoute actorInbound(HttpMethod method, String pathPattern, Workload issuer) {
        return new InboundRoute(
                Set.of(method),
                pathPattern,
                Set.of(issuer.id),
                ActorHeaderMode.REQUIRED,
                TrustedHeaderMode.FORBIDDEN);
    }

    private static ExemptRoute exemption(HttpMethod method, String pathPattern) {
        return new ExemptRoute(
                Set.of(method),
                pathPattern);
    }

    private static OutboundRoute outbound(HttpMethod method, String pathPattern, Workload audience) {
        return new OutboundRoute(
                Set.of(method),
                pathPattern,
                audience,
                ActorHeaderMode.FORBIDDEN);
    }

    private static OutboundRoute actorOutbound(HttpMethod method, String pathPattern, Workload audience) {
        return new OutboundRoute(
                Set.of(method),
                pathPattern,
                audience,
                ActorHeaderMode.REQUIRED);
    }

    private static Set<String> workloadIds(Workload... workloads) {
        Set<String> ids = new LinkedHashSet<>();
        for (Workload workload : workloads) {
            ids.add(workload.id);
        }

        return Set.copyOf(ids);
    }

    private enum TrustedHeaderMode {
        FORBIDDEN,
        ALLOWED
    }

    private enum SigningMode {
        SIGNER,
        VERIFIER_ONLY
    }

    private enum Workload {
        API_GATEWAY("api-gateway", SigningMode.SIGNER),
        INSTANCE_SERVICE("instance-service", SigningMode.SIGNER),
        USER_SERVICE("user-service", SigningMode.SIGNER),
        WORKSPACE_SERVICE("workspace-service", SigningMode.SIGNER),
        FILE_SERVICE("file-service", SigningMode.VERIFIER_ONLY),
        EMAIL_SERVICE("email-service", SigningMode.SIGNER);

        private final String id;
        private final SigningMode signingMode;

        Workload(String id, SigningMode signingMode) {
            this.id = id;
            this.signingMode = signingMode;
        }
    }

    @Getter
    public static final class InboundRoute {

        private final Set<HttpMethod> methods;
        private final String pathPattern;
        private final Set<String> issuerIds;
        private final ActorHeaderMode actorHeaderMode;
        private final TrustedHeaderMode trustedHeaderMode;

        private InboundRoute(
                Set<HttpMethod> methods,
                String pathPattern,
                Set<String> issuerIds,
                ActorHeaderMode actorHeaderMode,
                TrustedHeaderMode trustedHeaderMode) {
            Assert.notEmpty(methods, "methods are required");
            Assert.hasText(pathPattern, "pathPattern is required");
            Assert.notEmpty(issuerIds, "issuerIds are required");
            Assert.notNull(actorHeaderMode, "actorHeaderMode is required");
            Assert.notNull(trustedHeaderMode, "trustedHeaderMode is required");
            Assert.state(
                    trustedHeaderMode != TrustedHeaderMode.ALLOWED || issuerIds.equals(Set.of(Workload.API_GATEWAY.id)),
                    "trusted identity headers require the api-gateway issuer");

            this.methods = Set.copyOf(methods);
            this.pathPattern = pathPattern;
            this.issuerIds = Set.copyOf(issuerIds);
            this.actorHeaderMode = actorHeaderMode;
            this.trustedHeaderMode = trustedHeaderMode;
        }

        public boolean isTrustedHeadersAllowed() {
            return trustedHeaderMode == TrustedHeaderMode.ALLOWED;
        }

    }

    @Getter
    public static final class ExemptRoute {

        private final Set<HttpMethod> methods;
        private final String pathPattern;

        private ExemptRoute(Set<HttpMethod> methods, String pathPattern) {
            Assert.notEmpty(methods, "methods are required");
            Assert.hasText(pathPattern, "pathPattern is required");

            this.methods = Set.copyOf(methods);
            this.pathPattern = pathPattern;
        }

    }

    @Getter
    public static final class OutboundRoute {

        private final Workload audience;
        private final Set<HttpMethod> methods;
        private final String pathPattern;
        private final ActorHeaderMode actorHeaderMode;

        private OutboundRoute(
                Set<HttpMethod> methods,
                String pathPattern,
                Workload audience,
                ActorHeaderMode actorHeaderMode) {
            Assert.notNull(audience, "audience is required");
            Assert.notEmpty(methods, "methods are required");
            Assert.hasText(pathPattern, "pathPattern is required");
            Assert.notNull(actorHeaderMode, "actorHeaderMode is required");

            this.audience = audience;
            this.methods = Set.copyOf(methods);
            this.pathPattern = pathPattern;
            this.actorHeaderMode = actorHeaderMode;
        }

        public String getClientName() {
            return audience.id;
        }

        public String getAudience() {
            return audience.id;
        }

    }

}
