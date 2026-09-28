package com.syncturtle.common.security.autoconfigure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.syncturtle.common.security.property.RequestProvenanceKeyProperties;
import com.syncturtle.common.security.provenance.RequestProvenanceKeyLoader;
import com.syncturtle.common.security.provenance.RequestProvenancePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceProtocol;

class RequestProvenanceAutoConfigurationTest {

    private final RequestProvenanceAutoConfiguration configuration = new RequestProvenanceAutoConfiguration();
    private final RequestProvenanceKeyLoader keyLoader = new RequestProvenanceKeyLoader();

    @Test
    void signingWorkloadCannotStartWithoutSignerMaterial() {
        RequestProvenanceKeyProperties properties = properties(null, null, Map.of());

        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.userService(), properties, keyLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("signer keys are required");
    }

    @Test
    void verifierOnlyWorkloadCannotStartWithSignerMaterial() {
        ByteArrayResource privateKey = new ByteArrayResource(new byte[] { 1 });
        ByteArrayResource publicKey = new ByteArrayResource(new byte[] { 2 });
        RequestProvenanceKeyProperties properties = properties(privateKey, publicKey,
                Map.of("api-gateway", List.of(publicKey)));

        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.fileService(), properties, keyLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowed for verifier-only");
    }

    @Test
    void missingVerificationIssuerFailsStartup() throws Exception {
        KeyPair signerPair = keyPair();
        RequestProvenanceKeyProperties missing = properties(privateKey(signerPair), publicKey(signerPair), Map.of());

        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.emailService(), missing, keyLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-gateway");
    }

    @Test
    void AdditionalVerificationIssuerCannotCreateTrust() throws Exception {
        KeyPair signerPair = keyPair();
        Map<String, List<Resource>> additionalLocations = new LinkedHashMap<>();
        additionalLocations.put("api-gateway", List.of(publicKey(signerPair)));
        additionalLocations.put("random-service", List.of(publicKey(signerPair)));
        RequestProvenanceKeyProperties additional = properties(privateKey(signerPair), publicKey(signerPair),
                additionalLocations);

        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.emailService(), additional, keyLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("code-unauthorized issuer");
    }

    @Test
    void malformedAndDuplicatePublicKeysFailAsDeploymentState() throws Exception {
        KeyPair signerPair = keyPair();
        RequestProvenanceKeyProperties malformed = properties(
                privateKey(signerPair),
                publicKey(signerPair),
                Map.of("api-gateway", List.of(new ByteArrayResource(new byte[] { 1 }))));

        KeyPair verificationPair = keyPair();
        ByteArrayResource verificationPublicKey = publicKey(verificationPair);
        RequestProvenanceKeyProperties duplicate = properties(
                privateKey(signerPair),
                publicKey(signerPair),
                Map.of("api-gateway", List.of(verificationPublicKey, verificationPublicKey)));

        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.emailService(), malformed, keyLoader))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.requestProvenanceVerificationKeys(
                RequestProvenancePolicy.emailService(), duplicate, keyLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate request provenance key id");
    }

    private static RequestProvenanceKeyProperties properties(Resource privateKey, Resource PublicKey,
            Map<String, List<Resource>> verificationLocations) {
        return new RequestProvenanceKeyProperties(
                new RequestProvenanceKeyProperties.SignerMaterial(privateKey, PublicKey), verificationLocations);
    }

    private static KeyPair keyPair() throws Exception {
        return KeyPairGenerator
                .getInstance(RequestProvenanceProtocol.ALGORITHM)
                .generateKeyPair();
    }

    private static ByteArrayResource privateKey(KeyPair pair) {
        return new ByteArrayResource(pair.getPrivate().getEncoded());
    }

    private static ByteArrayResource publicKey(KeyPair pair) {
        return new ByteArrayResource(pair.getPublic().getEncoded());
    }

}
