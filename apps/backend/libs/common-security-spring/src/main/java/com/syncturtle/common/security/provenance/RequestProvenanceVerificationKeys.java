package com.syncturtle.common.security.provenance;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.property.RequestProvenanceKeyProperties;

public final class RequestProvenanceVerificationKeys {

    private final Map<String, Map<String, PublicKey>> keysByIssuer;

    public RequestProvenanceVerificationKeys(
            RequestProvenancePolicy policy,
            RequestProvenanceKeyProperties keyProperties,
            RequestProvenanceKeyLoader keyLoader) {
        Assert.notNull(policy, "policy is required");
        Assert.notNull(keyProperties, "keyProperties are required");
        Assert.notNull(keyLoader, "keyLoader is required");

        Map<String, List<Resource>> configured = keyProperties.getVerificationPublicKeyLocations();
        Set<String> expectedIssuers = policy.getTrustedIssuerIds();

        for (String configuredIssuer : configured.keySet()) {
            if (!expectedIssuers.contains(configuredIssuer)) {
                throw configuration(
                        "public keys configured for code-unauthorized issuer " + configuredIssuer);
            }
        }

        Map<String, Map<String, PublicKey>> issuers = new LinkedHashMap<>();
        for (String issuer : expectedIssuers) {
            List<Resource> locations = configured.get(issuer);
            if (locations == null || locations.isEmpty()) {
                throw configuration("at least one public key location is required for " + issuer);
            }

            Map<String, PublicKey> keys = new LinkedHashMap<>();
            for (Resource location : locations) {
                PublicKey publicKey = keyLoader.loadPublicKey(location);
                String keyId = keyLoader.keyId(publicKey);

                if (keys.putIfAbsent(keyId, publicKey) != null) {
                    throw configuration("duplicate request provenance key id for issuer " + issuer);
                }
            }

            issuers.put(issuer, Map.copyOf(keys));
        }

        this.keysByIssuer = Map.copyOf(issuers);
    }

    public PublicKey require(String issuer, String keyId) {
        Map<String, PublicKey> issuerKeys = keysByIssuer.get(issuer);
        if (issuerKeys == null) {
            throw new RequestProvenanceException("unknown request provenance issuer");
        }

        PublicKey key = issuerKeys.get(keyId);
        if (key == null) {
            throw new RequestProvenanceException("unknown request provenance key id");
        }

        return key;
    }

    private static IllegalStateException configuration(String message) {
        return new IllegalStateException(message);
    }

}
