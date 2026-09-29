package com.syncturtle.common.security.autoconfigure;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.security.property.RequestProvenanceKeyProperties;
import com.syncturtle.common.security.provenance.RequestProvenanceAssertionSerializer;
import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceKeyLoader;
import com.syncturtle.common.security.provenance.RequestProvenanceOutboundPolicy;
import com.syncturtle.common.security.provenance.RequestProvenancePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceRoutePolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;
import com.syncturtle.common.security.provenance.RequestProvenanceSigningKey;
import com.syncturtle.common.security.provenance.RequestProvenanceVerificationKeys;
import com.syncturtle.common.security.provenance.RequestProvenanceVerifier;

import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass(JsonMapper.class)
@ConditionalOnBean(RequestProvenancePolicy.class)
@EnableConfigurationProperties(RequestProvenanceKeyProperties.class)
public class RequestProvenanceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestProvenanceCanonicalizer requestProvenanceCanonicalizer() {
        return new RequestProvenanceCanonicalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestProvenanceAssertionSerializer requestProvenanceAssertionSerializer(JsonMapper jsonMapper) {
        return new RequestProvenanceAssertionSerializer(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    RequestProvenanceKeyLoader requestProvenanceKeyLoader() {
        return new RequestProvenanceKeyLoader();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.request-provenance.keys.signer", name = "private-key-location")
    RequestProvenanceSigningKey requestProvenanceSigningKey(
            RequestProvenanceKeyProperties keyProperties,
            RequestProvenanceKeyLoader keyLoader) {
        return keyLoader.loadSigningKey(
                keyProperties.getSigner().getPrivateKeyLocation(),
                keyProperties.getSigner().getPublicKeyLocation());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.request-provenance.keys.signer", name = "private-key-location")
    RequestProvenanceSigner requestProvenanceSigner(
            RequestProvenanceAssertionSerializer serializer,
            RequestProvenanceCanonicalizer canonicalizer,
            RequestProvenanceSigningKey signingKey,
            RequestProvenancePolicy policy,
            ObjectProvider<Clock> clocks) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);

        return new RequestProvenanceSigner(
                serializer,
                canonicalizer,
                signingKey,
                policy,
                clock);
    }

    @Bean
    RequestProvenanceVerificationKeys requestProvenanceVerificationKeys(
            RequestProvenancePolicy policy,
            RequestProvenanceKeyProperties keyProperties,
            RequestProvenanceKeyLoader keyLoader) {
        validateSignerMaterial(policy, keyProperties);

        return new RequestProvenanceVerificationKeys(
                policy,
                keyProperties,
                keyLoader);
    }

    @Bean
    RequestProvenanceVerifier requestProvenanceVerifier(
            RequestProvenanceAssertionSerializer serializer,
            RequestProvenanceCanonicalizer canonicalizer,
            RequestProvenanceVerificationKeys keys,
            RequestProvenancePolicy policy,
            ObjectProvider<Clock> clocks) {
        Clock clock = clocks.getIfAvailable(Clock::systemUTC);

        return new RequestProvenanceVerifier(
                serializer,
                canonicalizer,
                keys,
                policy,
                clock);
    }

    @Bean
    RequestProvenanceRoutePolicy requestProvenanceRoutePolicy(RequestProvenancePolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        return new RequestProvenanceRoutePolicy(policy, canonicalizer);
    }

    @Bean
    RequestProvenanceOutboundPolicy requestProvenanceOutboundPolicy(RequestProvenancePolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        return new RequestProvenanceOutboundPolicy(policy, canonicalizer);
    }

    private static void validateSignerMaterial(RequestProvenancePolicy policy,
            RequestProvenanceKeyProperties keyProperties) {
        boolean signerPresent = keyProperties.getSigner().isPresent();

        if (policy.isSigningRequired() && !signerPresent) {
            throw new IllegalStateException(
                    "request provenance signer keys are required for "
                            + policy.getWorkloadId());
        }

        if (!policy.isSigningRequired() && signerPresent) {
            throw new IllegalStateException(
                    "request provenance signer keys are not allowed for verifier-only workload "
                            + policy.getWorkloadId());
        }
    }

}
