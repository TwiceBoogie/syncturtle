package com.syncturtle.common.cache.unit.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;

import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;

@DisplayName("ResponseCacheKeyBuilderTest")
class ResponseCacheKeyBuilderTest {

    private final ResponseCacheKeyBuilder builder = new ResponseCacheKeyBuilder();

    @Nested
    @DisplayName("canonicalVariantInput(HttpServletRequest, String[])")
    class CanonicalVariantInputTests {

        @Test
        @DisplayName("includes HTTP method and request URI")
        void includesMethodAndRequestUri() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/instances");
            // conditions
            // act
            String actual = builder.canonicalVariantInput(request, new String[0]);
            // assert
            assertThat(actual).isEqualTo("GET /api/instances");
        }

        @Test
        @DisplayName("sorts query parameters by key and value")
        void sortsQueryParametersByKeyAndValue() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/instances/workspaces");
            request.addParameter("sort", "name");
            request.addParameter("sort", "createdAt");
            request.addParameter("page", "2");
            // conditions
            // act
            String actual = builder.canonicalVariantInput(request, new String[0]);
            // assert
            assertThat(actual).isEqualTo("GET /api/instances/workspaces?page=2&sort=createdAt&sort=name");
        }

        @Test
        @DisplayName("URL encodes query keys and values")
        void urlEncodesQueryKeysAndValues() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
            request.addParameter("q", "luna snow");
            request.addParameter("filter:type", "team/admin");
            // conditions
            // act
            String actual = builder.canonicalVariantInput(request, new String[0]);
            // assert
            assertThat(actual).isEqualTo("GET /api/users?filter%3Atype=team%2Fadmin&q=luna+snow");
        }

        @Test
        @DisplayName("sorts vary headers case insensitively and lowercases their names")
        void sortsVaryHeadersCaseInsensitivelyAndLowercasesNames() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
            request.addHeader("Accept-Language", "es-MX");
            request.addHeader("Accept-Language", "en-US");
            request.addHeader("X-Client", "admin");
            // conditions
            // act
            String actual = builder.canonicalVariantInput(request, new String[] { "X-Client", "Accept-Language" });
            // assert
            assertThat(actual).isEqualTo("GET /api/users#headers[accept-language=en-US%2Ces-MX&x-client=admin]");
        }

        @Test
        @DisplayName("ignores blank vary header names")
        void ignoresBlankVaryHeaderNames() {
            // arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
            request.addHeader("Accept-Language", "en-US");
            // conditions
            // act
            String actual = builder.canonicalVariantInput(request, new String[] { " ", "", "Accept-Language" });
            // assert
            assertThat(actual).isEqualTo("GET /api/users#headers[accept-language=en-US]");
        }

    }

    @Nested
    @DisplayName("variantHashHex(String, int)")
    class VariantHashHexTests {

        @Test
        @DisplayName("returns a stable lowercase hex hash")
        void returnsStableLowercaseHexHash() {
            // arrange
            String input = "GET /api/users?page=1";
            // conditions
            // act
            String first = builder.variantHashHex(input, 12);
            String second = builder.variantHashHex(input, 12);
            // assert
            assertThat(first).isEqualTo(second).hasSize(24).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("different canonical inputs produce different hashes")
        void differentInputsProduceDifferentHashes() {
            // arrange
            // conditions
            // act
            String first = builder.variantHashHex("GET /api/users?page=1", 12);
            String second = builder.variantHashHex("GET /api/users?page=2", 12);
            // assert
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("caps hash byte length at SHA-256 digest size")
        void capsHashByteLengthAtSha256DigestSize() {
            // arrange
            // conditions
            // act
            String actual = builder.variantHashHex("GET /api/users", 999);
            // assert
            assertThat(actual).hasSize(64);
        }

    }

    @Nested
    @DisplayName("versionKey(String, String, String)")
    class VersionKeyTests {

        @Test
        @DisplayName("builds a group generation key")
        void buildsGroupGenerationKey() {
            // arrange
            // conditions
            // act
            String actual = builder.versionKey("st:local:rc:", "instance-service", "public-instance");
            // assert
            assertThat(actual).isEqualTo("st:local:rc:instance-service:public-instance:ver");
        }

    }

    @Nested
    @DisplayName("responseKey(String, String, String, String, String, String, String)")
    class ResponseKeyTests {

        @Test
        @DisplayName("builds an unscoped response key")
        void buildsUnscopedResponseKey() {
            // arrange
            // conditions
            // act
            String actual = builder.responseKey("st:local:rc:", "instance-service", "public-instance", "7", "abc123",
                    "", "");
            // assert
            assertThat(actual).isEqualTo("st:local:rc:instance-service:public-instance:data:g7:h:abc123");
        }

        @Test
        @DisplayName("builds a workspace and user scoped response key")
        void buildsWorkspaceAndUserScopedResponseKey() {
            // arrange
            // conditions
            // act
            String actual = builder.responseKey("st:local:rc:", "instance-service", "public-instance", "7", "abc123",
                    builder.workspaceScope("workspace-1"), builder.userScope("user-1"));
            // assert
            assertThat(actual)
                    .isEqualTo("st:local:rc:instance-service:public-instance:data:g7:w:workspace-1:u:user-1:h:abc123");
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("invalidGenerations")
        @DisplayName("rejects invalid generations")
        void rejectsInvalidGenerations(String generation, String expectedMessage) {
            assertThatThrownBy(() -> builder.responseKey("st:local:rc:", "instance-service", "public-instance",
                    generation, "abc123", "", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage);
        }

        private static Stream<Arguments> invalidGenerations() {
            return Stream.of(
                    Arguments.of(null, "generation is required"),
                    Arguments.of("", "generation is required"),
                    Arguments.of(" ", "generation is required"),
                    Arguments.of("abc", "generation must be numeric"),
                    Arguments.of("1.5", "generation must be numeric"));
        }

    }

    @Nested
    @DisplayName("scope helpers")
    class ScopeHelperTests {

        @Test
        @DisplayName("builds user scope")
        void buildsUserScope() {
            assertThat(builder.userScope("user-1")).isEqualTo("u:user-1");
        }

        @Test
        @DisplayName("builds workspace scope")
        void buildsWorkspaceScope() {
            assertThat(builder.workspaceScope("workspace-1")).isEqualTo("w:workspace-1");
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("invalidScopeValues")
        @DisplayName("rejects invalid scope values")
        void rejectsInvalidScopeValues(String value, String expectedMessage) {
            assertThatThrownBy(() -> builder.userScope(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage);
        }

        private static Stream<Arguments> invalidScopeValues() {
            return Stream.of(
                    Arguments.of(null, "userId is required"),
                    Arguments.of("", "userId is required"),
                    Arguments.of(" ", "userId is required"),
                    Arguments.of("user 1", "userId must not contain spaces"),
                    Arguments.of("user:1", "userId must not contain ':'"));
        }

    }

}
