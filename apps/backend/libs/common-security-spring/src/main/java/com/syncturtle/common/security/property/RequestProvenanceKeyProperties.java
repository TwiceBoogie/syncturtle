package com.syncturtle.common.security.property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.security.request-provenance.keys", ignoreUnknownFields = false)
public final class RequestProvenanceKeyProperties {

    private final SignerMaterial signer;
    private final Map<String, List<Resource>> verificationPublicKeyLocations;

    public RequestProvenanceKeyProperties(@DefaultValue SignerMaterial signer,
            Map<String, List<Resource>> verificationPublicKeyLocations) {
        this.signer = signer;
        this.verificationPublicKeyLocations = immutableVerificationLocations(verificationPublicKeyLocations);
    }

    private static Map<String, List<Resource>> immutableVerificationLocations(
            Map<String, List<Resource>> locations) {
        if (locations == null || locations.isEmpty()) {
            return Map.of();
        }

        Map<String, List<Resource>> copy = new LinkedHashMap<>(locations.size());
        locations.forEach((issuer, issuerLocations) -> {
            if (issuer == null || issuer.isBlank() || !issuer.equals(issuer.trim())) {
                throw invalid("verification issuer is required and must be canonical");
            }

            if (issuerLocations == null || issuerLocations.isEmpty()) {
                throw invalid("at least one public key location is required for " + issuer);
            }

            if (issuerLocations.stream().anyMatch(location -> location == null)) {
                throw invalid("null public key location for " + issuer);
            }

            copy.put(issuer, List.copyOf(issuerLocations));
        });

        return Map.copyOf(copy);
    }

    private static IllegalStateException invalid(String detail) {
        return new IllegalStateException("app.security.request-provenance.keys." + detail);
    }

    @Getter
    public static final class SignerMaterial {

        private final Resource privateKeyLocation;
        private final Resource publicKeyLocation;

        public SignerMaterial(Resource privateKeyLocation, Resource publicKeyLocation) {
            if ((privateKeyLocation == null) != (publicKeyLocation == null)) {
                throw invalid("signer private/public key locations must be configured together");
            }

            this.privateKeyLocation = privateKeyLocation;
            this.publicKeyLocation = publicKeyLocation;
        }

        public boolean isPresent() {
            return privateKeyLocation != null;
        }

    }

}
