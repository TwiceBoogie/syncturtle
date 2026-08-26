package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;

class RefreshSessionClientFingerprintFactoryTest {

    private RefreshSessionClientFingerprintFactory factory;

    @BeforeEach
    void setUp() {
        RefreshSessionClientBindingProperties properties = new RefreshSessionClientBindingProperties(
                "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8");
        factory = new RefreshSessionClientFingerprintFactory(properties);
    }

    @Nested
    class Create {

        @Test
        void producesStableBindingFromCanonicalClientMetadata() {
            // arrange
            String clientIp = " 2001:DB8::1 ";
            String userAgent = " Test\tBrowser/1.0 ";

            // act
            RefreshSessionClientFingerprint first = factory.create(clientIp, userAgent);
            RefreshSessionClientFingerprint second = factory.create("2001:db8::1", "test browser/1.0");

            // assert
            assertThat(first.getClientBindingHash()).hasSize(64).isEqualTo(second.getClientBindingHash());
            assertThat(first.getDeviceLabel()).isEqualTo("Test Browser/1.0");
        }

        @Test
        void changesBindingWhenEitherClientComponentChanges() {
            // arrange and act
            RefreshSessionClientFingerprint baseline = factory.create("192.0.2.10", "Browser A");
            RefreshSessionClientFingerprint otherIp = factory.create("192.0.2.11", "Browser A");
            RefreshSessionClientFingerprint otherAgent = factory.create("192.0.2.10", "Browser B");

            // assert
            assertThat(otherIp.getClientBindingHash()).isNotEqualTo(baseline.getClientBindingHash());
            assertThat(otherAgent.getClientBindingHash()).isNotEqualTo(baseline.getClientBindingHash());
        }

        @Test
        void boundsDisplayAndBindingInputsWithoutPersistingRawMetadata() {
            // arrange
            String longAgent = "Browser " + "x".repeat(800);

            // act
            RefreshSessionClientFingerprint result = factory.create(null, longAgent);

            // assert
            assertThat(result.getDeviceLabel()).hasSize(120);
            assertThat(result.getClientBindingHash()).hasSize(64).doesNotContain("Browser");
        }
    }
}
