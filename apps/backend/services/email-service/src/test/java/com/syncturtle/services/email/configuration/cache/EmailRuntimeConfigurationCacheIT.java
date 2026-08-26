package com.syncturtle.services.email.configuration.cache;

import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.completeRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.runtimeConfigResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.syncturtle.services.email.client.InstanceClient;
import com.syncturtle.services.email.configuration.property.EmailRuntimeConfigCacheProperties;
import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigResolver;
import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigSnapshot;

@DisplayName("Email runtime configuration cache")
class EmailRuntimeConfigurationCacheIT {

    @Nested
    @DisplayName("cache policy")
    class CachePolicy {

        @Test
        @DisplayName("keeps only the current entry")
        void keepsOnlyCrrentEntry() {
            try (AnnotationConfigApplicationContext context = context(Duration.ofMinutes(1))) {
                Cache<String, EmailRuntimeConfigSnapshot> cache = cache(context);

                cache.put("first", completeRuntimeConfig());
                cache.put("second", completeRuntimeConfig());
                cache.cleanUp();

                assertThat(cache.estimatedSize()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("configures expiry after the requested write ttl")
        void configuresExpiryAfterWrite() {
            try (AnnotationConfigApplicationContext context = context(Duration.ofSeconds(5))) {
                Cache<String, EmailRuntimeConfigSnapshot> cache = cache(context);

                long configuredMillis = cache.policy()
                        .expireAfterWrite()
                        .orElseThrow()
                        .getExpiresAfter(TimeUnit.MILLISECONDS);

                assertThat(configuredMillis).isEqualTo(5_000);
            }
        }

    }

    @Nested
    @DisplayName("resolver interaction")
    class ResolverInteraction {

        @Test
        @DisplayName("coalesces simultaneous cache misses into one remote fetch")
        void coalescesSimultaneousMisses() throws Exception {
            // arrange
            InstanceClient client = mock(InstanceClient.class);
            Cache<String, EmailRuntimeConfigSnapshot> cache = Caffeine.newBuilder().maximumSize(1).build();
            EmailRuntimeConfigResolver resolver = new EmailRuntimeConfigResolver(client, cache);
            CountDownLatch fetchEntered = new CountDownLatch(1);
            CountDownLatch allowFetchToReturn = new CountDownLatch(1);
            // conditions
            when(client.getRuntimeEmailConfig()).thenAnswer(invocation -> {
                fetchEntered.countDown();
                if (!allowFetchToReturn.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release runtime-config fetch");
                }
                return runtimeConfigResponse();
            });

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<EmailRuntimeConfigSnapshot> first = executor.submit(resolver::resolveCurrent);
                assertThat(fetchEntered.await(5, TimeUnit.SECONDS)).isTrue();
                Future<EmailRuntimeConfigSnapshot> second = executor.submit(resolver::resolveCurrent);

                allowFetchToReturn.countDown();

                assertThat(first.get(5, TimeUnit.SECONDS)).isSameAs(second.get(5, TimeUnit.SECONDS));
            }

            verify(client).getRuntimeEmailConfig();
        }

        @Test
        @DisplayName("fetches again after the configured cache entry expires")
        void fetchesAgainAfterExpiry() {
            MutableTicker ticker = new MutableTicker();
            Cache<String, EmailRuntimeConfigSnapshot> cache = Caffeine.newBuilder()
                    .maximumSize(1)
                    .expireAfterWrite(Duration.ofSeconds(3))
                    .ticker(ticker)
                    .build();
            InstanceClient client = mock(InstanceClient.class);
            EmailRuntimeConfigResolver resolver = new EmailRuntimeConfigResolver(client, cache);
            // conditions
            when(client.getRuntimeEmailConfig()).thenReturn(runtimeConfigResponse());
            // act
            EmailRuntimeConfigSnapshot first = resolver.resolveCurrent();
            ticker.advance(Duration.ofSeconds(6));
            cache.cleanUp();
            EmailRuntimeConfigSnapshot second = resolver.resolveCurrent();
            // assert
            assertThat(first).isNotSameAs(second);
            // verify
            verify(client, times(2)).getRuntimeEmailConfig();
        }

    }

    private static AnnotationConfigApplicationContext context(Duration ttl) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(EmailRuntimeConfigCacheProperties.class, () -> new EmailRuntimeConfigCacheProperties(ttl));
        context.register(EmailRuntimeConfigCacheConfiguration.class);
        context.refresh();
        return context;
    }

    @SuppressWarnings("unchecked")
    private static Cache<String, EmailRuntimeConfigSnapshot> cache(AnnotationConfigApplicationContext context) {
        return context.getBean(Cache.class);
    }

    private static final class MutableTicker implements Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }

}
