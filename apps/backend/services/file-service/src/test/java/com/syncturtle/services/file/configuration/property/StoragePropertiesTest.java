package com.syncturtle.services.file.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class StoragePropertiesTest {

    @Test
    void acceptsExplicitDeploymentCredentialsAndIntrinsicDefaults() {
        StorageProperties properties = new StorageProperties(
                "fake-access-key",
                "fake-secret-key",
                "us-east-1",
                "syncturtle-test",
                URI.create("http://localhost:9000"),
                URI.create("http://localhost"),
                true,
                Duration.ofHours(1));

        assertThat(properties.getClientUploadEndpoint())
                .isEqualTo(URI.create("http://localhost/syncturtle-test"));
    }
}
