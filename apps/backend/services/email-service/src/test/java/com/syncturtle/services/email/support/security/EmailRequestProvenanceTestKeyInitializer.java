package com.syncturtle.services.email.support.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import com.syncturtle.common.security.provenance.RequestProvenanceProtocol;

public final class EmailRequestProvenanceTestKeyInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final TestKeyLocations KEY_LOCATIONS = createTestKeyLocations();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertyValues.of(
                "app.security.request-provenance.keys.signer.private-key-location="
                        + KEY_LOCATIONS.emailPrivateKeyLocation,
                "app.security.request-provenance.keys.signer.public-key-location="
                        + KEY_LOCATIONS.emailPublicKeyLocation,
                "app.security.request-provenance.keys.verification-public-key-locations.api-gateway[0]="
                        + KEY_LOCATIONS.gatewayPublicKeyLocation)
                .applyTo(applicationContext.getEnvironment());
    }

    private static TestKeyLocations createTestKeyLocations() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM);
            KeyPair emailKeyPair = generator.generateKeyPair();
            KeyPair gatewayKeyPair = generator.generateKeyPair();

            Path emailPrivateKey = writeTemporaryKey("syncturtle-email-provenance-private-",
                    emailKeyPair.getPrivate().getEncoded());
            Path emailPublicKey = writeTemporaryKey("syncturtle-email-provenance-public-",
                    emailKeyPair.getPublic().getEncoded());
            Path gatewayPublicKey = writeTemporaryKey("syncturtle-gateway-provenance-public-",
                    gatewayKeyPair.getPublic().getEncoded());

            return new TestKeyLocations(
                    emailPrivateKey.toUri().toString(),
                    emailPublicKey.toUri().toString(),
                    gatewayPublicKey.toUri().toString());
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("failed to create email-service request provenance test keys", exception);
        }
    }

    private static Path writeTemporaryKey(String prefix, byte[] encodedKey) throws IOException {
        Path keyPath = Files.createTempFile(prefix, ".der");
        Files.write(keyPath, encodedKey);
        keyPath.toFile().deleteOnExit();
        return keyPath;
    }

    private static final class TestKeyLocations {

        private final String emailPrivateKeyLocation;
        private final String emailPublicKeyLocation;
        private final String gatewayPublicKeyLocation;

        private TestKeyLocations(
                String emailPrivateKeyLocation,
                String emailPublicKeyLocation,
                String gatewayPublicKeyLocation) {
            this.emailPrivateKeyLocation = emailPrivateKeyLocation;
            this.emailPublicKeyLocation = emailPublicKeyLocation;
            this.gatewayPublicKeyLocation = gatewayPublicKeyLocation;
        }

    }

}
