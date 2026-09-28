package com.syncturtle.common.security.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.property.RequestProvenanceKeyProperties;
import com.syncturtle.common.security.type.ActorHeaderMode;

class RequestProvenanceArchitectureTest {

    @Nested
    class ProtocolTests {

        @Test
        void fixesVersionAlgorithmLifetimeSkewAndBoundsInCode() {
            assertThat(RequestProvenanceProtocol.VERSION).isEqualTo(1);
            assertThat(RequestProvenanceProtocol.ALGORITHM).isEqualTo("Ed25519");
            assertThat(RequestProvenanceProtocol.ASSERTION_LIFETIME).isEqualTo(Duration.ofSeconds(10));
            assertThat(RequestProvenanceProtocol.ALLOWED_CLOCK_SKEW).isEqualTo(Duration.ofSeconds(5));
            assertThat(RequestProvenanceProtocol.MAX_HEADER_CHARACTERS).isEqualTo(4096);
            assertThat(RequestProvenanceProtocol.MAX_PAYLOAD_BYTES).isEqualTo(2048);
            assertThat(RequestProvenanceProtocol.MAX_RAW_PATH_CHARACTERS).isEqualTo(2048);
            assertThat(RequestProvenanceProtocol.MAX_RAW_QUERY_CHARACTERS).isEqualTo(4096);
            assertThat(RequestProvenanceProtocol.ED25519_SIGNATURE_BYTES).isEqualTo(64);
            assertThat(RequestProvenanceProtocol.EXPECTED_FIELDS)
                    .containsExactlyInAnyOrder("v", "alg", "kid", "iss", "aud", "iat", "exp", "m", "p", "q", "h");
        }

        @Test
        void keepsProtectedSecurityHeadersInCode() {
            RequestProvenanceCanonicalizer canonicalizer = new RequestProvenanceCanonicalizer();

            assertThat(canonicalizer.protectedHeaderNames())
                    .containsExactlyInAnyOrder(
                            lower(GatewayHeaders.HDR_AUTH_USER_ID),
                            lower(GatewayHeaders.HDR_AUTH_SESSION_ID),
                            lower(GatewayHeaders.HDR_AUTH_INSTANCE_ID),
                            lower(GatewayHeaders.HDR_AUTH_ROLES),
                            lower(GatewayHeaders.HDR_AUTH_USER_AUTH_VERSION),
                            lower(GatewayHeaders.HDR_AUTH_ADMIN_SESSION_VERSION),
                            lower(GatewayHeaders.HDR_AUTH_ISSUER),
                            lower(GatewayHeaders.HDR_AUTH_WORKSPACE_ID),
                            lower(GatewayHeaders.HDR_CLIENT_IP),
                            lower(GatewayHeaders.HDR_CLIENT_UA),
                            lower(GatewayHeaders.HDR_CLIENT_DEVICE_ID),
                            lower(GatewayHeaders.HDR_CLIENT_LOCALE),
                            lower(GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING),
                            lower(GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID),
                            lower(GatewayHeaders.HDR_INTERNAL_LOGIN_CONTEXT),
                            lower(GatewayHeaders.HDR_INTERNAL_LOGOUT_CONTEXT),
                            lower(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID));
        }

    }

    @Nested
    class PolicyTests {

        @Test
        void ownsExactlyTwelveInternalOutboundCallsAndTwoActorCalls() {
            List<RequestProvenancePolicy> policies = List.of(
                    RequestProvenancePolicy.instanceService(),
                    RequestProvenancePolicy.userService(),
                    RequestProvenancePolicy.workspaceService(),
                    RequestProvenancePolicy.fileService(),
                    RequestProvenancePolicy.emailService());
            List<RequestProvenancePolicy.OutboundRoute> outboundRoutes = new ArrayList<>();

            for (RequestProvenancePolicy policy : policies) {
                outboundRoutes.addAll(policy.getOutboundRoutes());
            }

            long actorRoutes = outboundRoutes.stream()
                    .filter(route -> route.getActorHeaderMode() == ActorHeaderMode.REQUIRED)
                    .count();
            long actorInboundRoutes = RequestProvenancePolicy.workspaceService().getInboundRoutes().stream()
                    .filter(route -> route.getActorHeaderMode() == ActorHeaderMode.REQUIRED)
                    .count();

            assertThat(outboundRoutes).hasSize(12);
            assertThat(actorRoutes).isEqualTo(2);
            assertThat(actorInboundRoutes).isEqualTo(2);
            assertThat(RequestProvenancePolicy.fileService().isSigningRequired()).isFalse();
        }

        @Test
        void derivesFeignTargetNameAndAudienceFromOneDestinationWorkload() {
            for (RequestProvenancePolicy.OutboundRoute route : RequestProvenancePolicy.instanceService()
                    .getOutboundRoutes()) {
                assertThat(route.getClientName()).isEqualTo(route.getAudience());
            }
        }
    }

    @Nested
    class KeyMaterialTests {

        @Test
        void containsOnlyDeploymentMaterialAndDefensivelyCopiesIt() {
            assertThat(Arrays.stream(RequestProvenanceKeyProperties.class.getDeclaredFields())
                    .map(Field::getName)
                    .toList())
                    .containsExactlyInAnyOrder("signer", "verificationPublicKeyLocations");

            Map<String, List<Resource>> locations = new LinkedHashMap<>();
            locations.put("api-gateway", List.of(new ByteArrayResource(new byte[] { 1 })));
            RequestProvenanceKeyProperties properties = new RequestProvenanceKeyProperties(
                    new RequestProvenanceKeyProperties.SignerMaterial(null, null),
                    locations);
            locations.clear();

            assertThat(properties.getVerificationPublicKeyLocations()).containsOnlyKeys("api-gateway");
            assertThatThrownBy(() -> properties.getVerificationPublicKeyLocations().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejectsPartialSignerMaterialAtConstruction() {
            assertThatThrownBy(() -> new RequestProvenanceKeyProperties.SignerMaterial(
                    new ByteArrayResource(new byte[] { 1 }),
                    null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void missingExpectedIssuerFailsStartup() throws Exception {
            KeyPair pair = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM).generateKeyPair();
            ByteArrayResource privateKey = new ByteArrayResource(pair.getPrivate().getEncoded());
            ByteArrayResource publicKey = new ByteArrayResource(pair.getPublic().getEncoded());
            RequestProvenancePolicy policy = RequestProvenancePolicy.emailService();
            RequestProvenanceKeyLoader keyLoader = new RequestProvenanceKeyLoader();
            RequestProvenanceKeyProperties missing = new RequestProvenanceKeyProperties(
                    new RequestProvenanceKeyProperties.SignerMaterial(privateKey, publicKey),
                    Map.of());

            assertThatThrownBy(() -> new RequestProvenanceVerificationKeys(policy, missing, keyLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("api-gateway");
        }

        @Test
        void additionalIssuerCannotExpandCodeOwnedTrust() throws Exception {
            KeyPair pair = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM).generateKeyPair();
            ByteArrayResource privateKey = new ByteArrayResource(pair.getPrivate().getEncoded());
            ByteArrayResource publicKey = new ByteArrayResource(pair.getPublic().getEncoded());
            RequestProvenancePolicy policy = RequestProvenancePolicy.emailService();
            RequestProvenanceKeyLoader keyLoader = new RequestProvenanceKeyLoader();
            Map<String, List<Resource>> additionalLocations = new LinkedHashMap<>();
            additionalLocations.put("api-gateway", List.of(publicKey));
            additionalLocations.put("random-service", List.of(publicKey));
            RequestProvenanceKeyProperties additional = new RequestProvenanceKeyProperties(
                    new RequestProvenanceKeyProperties.SignerMaterial(privateKey, publicKey),
                    additionalLocations);

            assertThatThrownBy(() -> new RequestProvenanceVerificationKeys(policy, additional, keyLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("code-unauthorized issuer random-service");
        }

        @Test
        void malformedConfiguredPublicKeyIsAStartupStateFailure() {
            RequestProvenancePolicy policy = RequestProvenancePolicy.fileService();
            RequestProvenanceKeyProperties properties = new RequestProvenanceKeyProperties(
                    new RequestProvenanceKeyProperties.SignerMaterial(null, null),
                    Map.of("api-gateway", List.of(new ByteArrayResource(new byte[] { 1 }))));

            assertThatThrownBy(() -> new RequestProvenanceVerificationKeys(
                    policy,
                    properties,
                    new RequestProvenanceKeyLoader()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private static String lower(String headerName) {
        return headerName.toLowerCase(Locale.ROOT);
    }

}
