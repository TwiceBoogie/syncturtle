package com.syncturtle.common.cache.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.syncturtle.common.cache.property.RedisKeyProperties;

@DisplayName("RedisKeyBuilder")
class RedisKeyBuilderTest {

    private final RedisKeyBuilder builder = new RedisKeyBuilder(properties("st:local:"));

    @Nested
    @DisplayName("key(String, String, String, String...)")
    class KeyTests {

        @Test
        @DisplayName("builds generic redis key")
        void buildsGenericRedisKey() {
            String actual = builder.key("cfg", "instance-service", "auth", "enable-signup");

            assertThat(actual).isEqualTo("st:local:cfg:instance-service:auth:enable-signup");
        }

        @Test
        @DisplayName("builds key without details")
        void buildsKeyWithoutDetails() {
            String actual = builder.key("cfg", "instance-service", "auth");

            assertThat(actual).isEqualTo("st:local:cfg:instance-service:auth");
        }

        @Test
        @DisplayName("ignores null details array")
        void ignoresNullDetailsArray() {
            String actual = builder.key("cfg", "instance-service", "auth", (String[]) null);

            assertThat(actual).isEqualTo("st:local:cfg:instance-service:auth");
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("invalidSegments")
        @DisplayName("rejects invalid key segments")
        void rejectsInvalidKeySegments(String namespace, String owner, String expectedMessage) {
            assertThatThrownBy(() -> builder.key(namespace, owner, "auth"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage);
        }

        private static Stream<Arguments> invalidSegments() {
            return Stream.of(
                    Arguments.of(null, "instance-service", "Redis key namespace is required"),
                    Arguments.of("", "instance-service", "Redis key namespace is required"),
                    Arguments.of(" ", "instance-service", "Redis key namespace is required"),
                    Arguments.of("c:fg", "instance-service", "Redis key namespace must not contain ':'"),
                    Arguments.of("c fg", "instance-service", "Redis key namespace must not contain spaces"),
                    Arguments.of("cfg", null, "Redis key owner is required"),
                    Arguments.of("cfg", "instance service", "Redis key owner must not contain spaces"),
                    Arguments.of("cfg", "instance:service", "Redis key owner must not contain ':'"));
        }

    }

    @Nested
    @DisplayName("semantic key helpers")
    class SemanticKeyHelperTests {

        @Test
        @DisplayName("builds auth key")
        void buildsAuthKey() {
            assertThat(builder.authKey("user-service", "session", "session-123"))
                    .isEqualTo("st:local:auth:user-service:session:session-123");
        }

        @Test
        @DisplayName("builds config key")
        void buildsConfigKey() {
            assertThat(builder.configKey("instance-service", "auth", "enable-signup"))
                    .isEqualTo("st:local:cfg:instance-service:auth:enable-signup");
        }

        @Test
        @DisplayName("builds projection key")
        void buildsProjectionKey() {
            assertThat(builder.projectionKey("workspace-service", "users-lite", "user-123"))
                    .isEqualTo("st:local:proj:workspace-service:users-lite:user-123");
        }

        @Test
        @DisplayName("builds lock key")
        void buildsLockKey() {
            assertThat(builder.lockKey("file-service", "upload", "asset-123"))
                    .isEqualTo("st:local:lock:file-service:upload:asset-123");
        }

        @Test
        @DisplayName("builds idempotency key")
        void buildsIdempotencyKey() {
            assertThat(builder.idempotencyKey("email-service", "send", "event-123"))
                    .isEqualTo("st:local:idem:email-service:send:event-123");
        }

        @Test
        @DisplayName("builds rate limit key")
        void buildsRateLimitKey() {
            assertThat(builder.rateLimitKey("api-gateway", "login", "ip-127-0-0-1"))
                    .isEqualTo("st:local:rl:api-gateway:login:ip-127-0-0-1");
        }

    }

    private static RedisKeyProperties properties(String keyPrefix) {
        RedisKeyProperties properties = new RedisKeyProperties(keyPrefix);
        return properties;
    }

}
