package com.syncturtle.services.email.service.runtime;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.VERSION;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.completeRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.runtimeConfigResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.services.email.client.InstanceClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailRuntimeConfigResolver")
class EmailRuntimeConfigResolverTest {

    @Mock
    InstanceClient instanceClient;
    @Mock
    Cache<String, EmailRuntimeConfigSnapshot> cache;

    private EmailRuntimeConfigResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new EmailRuntimeConfigResolver(instanceClient, cache);
    }

    @Nested
    @DisplayName("resolveCurrent()")
    class ResolveCurrentTests {

        @Test
        @DisplayName("returns cached snapshot without remote fetch")
        void returnsCachedSnapshotWithoutRemoteFetch() {
            // arrange
            EmailRuntimeConfigSnapshot cached = completeRuntimeConfig();
            // conditions
            when(cache.getIfPresent("current")).thenReturn(cached);
            // act
            EmailRuntimeConfigSnapshot result = resolver.resolveCurrent();
            // assert
            assertThat(result).isSameAs(cached);
            // verify
            verify(cache).getIfPresent("current");
            verifyNoInteractions(instanceClient);
        }

        @Test
        @DisplayName("fetches, maps, and caches snapshot on cache miss")
        void fetchesMapsAndCachesSnapshotOnCacheMiss() {
            // arrange
            // conditions
            when(cache.getIfPresent("current")).thenReturn(null);
            when(instanceClient.getRuntimeEmailConfig()).thenReturn(runtimeConfigResponse());
            // act
            EmailRuntimeConfigSnapshot result = resolver.resolveCurrent();
            // assert
            assertThat(result.getVersion()).isEqualTo(VERSION);
            assertThat(result.isComplete()).isTrue();
            // verify
            verify(instanceClient).getRuntimeEmailConfig();
            verify(cache).put("current", result);
        }

        @Test
        @DisplayName("wraps remote failure")
        void wrapsRemoteFailure() {
            // arrange
            RuntimeException cause = new RuntimeException("instance service unavailable");
            // conditions
            when(cache.getIfPresent("current")).thenReturn(null);
            when(instanceClient.getRuntimeEmailConfig()).thenThrow(cause);
            // act + assert
            assertThatEmailExceptionThrownBy(resolver::resolveCurrent)
                    .hasErrorCode(EmailErrorCode.EMAIL_RUNTIME_CONFIG_FETCH_FAILED)
                    .hasCauseSameAs(cause);
        }

    }

    @Nested
    @DisplayName("refreshIfOlderThan(long)")
    class RefreshIfOlderThanTests {

        @Test
        @DisplayName("returns cache when version satisfies requirement")
        void returnsCacheWhenVersionSatisfiesRequirement() {
            // arrange
            EmailRuntimeConfigSnapshot cached = completeRuntimeConfig();
            // conditions
            when(cache.getIfPresent("current")).thenReturn(cached);
            // act
            EmailRuntimeConfigSnapshot result = resolver.refreshIfOlderThan(VERSION);
            // assert
            assertThat(result).isSameAs(cached);
            // verify
            verifyNoInteractions(instanceClient);
        }

        @Test
        @DisplayName("throws stale error when fresh response is still older")
        void throwsStaleErrorWhenFreshResponseIsStillOlder() {
            // arrange
            // conditions
            when(cache.getIfPresent("current")).thenReturn(null);
            when(instanceClient.getRuntimeEmailConfig()).thenReturn(runtimeConfigResponse());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> resolver.refreshIfOlderThan(VERSION + 1))
                    .hasErrorCode(EmailErrorCode.EMAIL_RUNTIME_CONFIG_STALE);
            // verify
        }

        @Test
        @DisplayName("rejects negative required version")
        void rejectsNegativeRequiredVersion() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> resolver.refreshIfOlderThan(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("requiredScopeVersion must not be negative");
            // verify
            verifyNoInteractions(cache, instanceClient);
        }

    }

    @Nested
    @DisplayName("evict()")
    class EvictTests {

        @Test
        @DisplayName("invalidates current cache key")
        void invalidatesCurrentCacheKey() {
            // arrange
            // conditions
            // act
            resolver.evict();
            // assert
            // verify
            verify(cache).invalidate("current");
            verifyNoInteractions(instanceClient);
        }

    }

}
