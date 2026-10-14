package com.syncturtle.services.instance.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import com.syncturtle.services.instance.configuration.property.BinaryVersionProperties;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;

@DisplayName("Current version resolver")
class CurrentVersionResolverTest {

    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Nested
    class Resolve {

        @Test
        void validAppVersionOverridesBuildMetadata() {
            InstanceBinaryParam binary = resolver("v1.2.4", "1.2.3").resolve(NOW);

            assertThat(binary.getBinaryVersion()).isEqualTo("v1.2.4");
        }

        @Test
        void usesBuildMetadataWhenOverrideIsAbsentOrMalformed() {
            assertThat(resolver("", "1.2.3").resolve(NOW).getBinaryVersion()).isEqualTo("1.2.3");
            assertThat(resolver("bad-release", "1.2.3").resolve(NOW).getBinaryVersion())
                    .isEqualTo("1.2.3");
        }

        @Test
        void usesExplicitDevelopmentStateWhenNeitherVersionIsUsable() {
            InstanceBinaryParam binary = resolver(null, null).resolve(NOW);

            assertThat(binary.getBinaryVersion()).isEqualTo("development");
            assertThat(binary.getBinaryBuiltAt()).isEqualTo(NOW);
        }
    }

    @SuppressWarnings("unchecked")
    private static CurrentVersionResolver resolver(String override, String buildVersion) {
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        if (buildVersion != null) {
            BuildProperties build = mock(BuildProperties.class);
            when(build.getVersion()).thenReturn(buildVersion);
            when(provider.getIfAvailable()).thenReturn(build);
        }
        return new CurrentVersionResolver(new BinaryVersionProperties(override), provider);
    }

}
