package com.syncturtle.services.user.configuration.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;

class UserAuthRuntimeCacheConfigurationTest {

    @Nested
    class ValueSerializer {

        @Test
        void roundTripsRuntimeSnapshotAsItsDeclaredType() {
            UserAuthRuntimeSnapshot snapshot = UserAuthRuntimeSnapshot.builder()
                    .signupEnabled(true)
                    .magicLinkEnabled(false)
                    .emailPasswordEnabled(true)
                    .smtpEnabled(true)
                    .googleEnabled(false)
                    .githubEnabled(true)
                    .gitlabEnabled(false)
                    .version(7L)
                    .build();
            RedisSerializer<UserAuthRuntimeSnapshot> serializer = UserAuthRuntimeCacheConfiguration.valueSerializer();

            byte[] serialized = serializer.serialize(snapshot);
            UserAuthRuntimeSnapshot deserialized = serializer.deserialize(serialized);

            assertThat(deserialized).isNotNull();
            assertThat(deserialized.isSignupEnabled()).isTrue();
            assertThat(deserialized.isMagicLinkEnabled()).isFalse();
            assertThat(deserialized.isEmailPasswordEnabled()).isTrue();
            assertThat(deserialized.isSmtpEnabled()).isTrue();
            assertThat(deserialized.isGoogleEnabled()).isFalse();
            assertThat(deserialized.isGithubEnabled()).isTrue();
            assertThat(deserialized.isGitlabEnabled()).isFalse();
            assertThat(deserialized.getVersion()).isEqualTo(7L);
        }
    }
}
