package com.syncturtle.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.services.email.clients.InstanceClient;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exceptions.EmailRuntimeConfigException;
import com.syncturtle.services.email.service.EmailRuntimeConfigService;

@ExtendWith(MockitoExtension.class)
class EmailRuntimeConfigServiceTest {

    @Mock
    InstanceClient instanceClient;

    Cache<String, EmailRuntimeConfig> cache;
    EmailRuntimeConfigService service;

    @BeforeEach
    void setup() {
        cache = Caffeine.newBuilder().maximumSize(10).build();
        service = new EmailRuntimeConfigService(instanceClient, cache);
    }

    @Test
    void getCurrentConfig_whenCacheMiss_fetchesAndCaches() {
        // arrange
        when(instanceClient.getRuntimeEmailConfig()).thenReturn(response(3L));

        // act
        EmailRuntimeConfig first = service.getCurrentConfig();
        EmailRuntimeConfig second = service.getCurrentConfig();

        // assert
        assertThat(first.getVersion()).isEqualTo(3L);
        assertThat(second.getVersion()).isEqualTo(3L);
        assertThat(second).isSameAs(first);

        // verify
        verify(instanceClient).getRuntimeEmailConfig();
        verifyNoMoreInteractions(instanceClient);
    }

    @Test
    void refreshIfOlderThan_whenCachedVersionIsFresh_returnsCachedWithoutFetch() {
        // arrange
        cache.put("current", runtimeConfig(10L));
        // act
        EmailRuntimeConfig result = service.refreshIfOlderThan(5L);
        // assert
        assertThat(result.getVersion()).isEqualTo(10L);
        // verify
        verifyNoMoreInteractions(instanceClient);
    }

    @Test
    void refreshIfOlderThan_whenCachedVersionIsStale_fetchesAndReplacesCache() {
        // arrange
        cache.put("current", runtimeConfig(2L));
        when(instanceClient.getRuntimeEmailConfig()).thenReturn(response(8L));
        // act
        EmailRuntimeConfig result = service.refreshIfOlderThan(5L);
        // assert
        assertThat(result.getVersion()).isEqualTo(8L);
        assertThat(cache.getIfPresent("current")).isSameAs(result);
        // verify
        verify(instanceClient).getRuntimeEmailConfig();
        verifyNoMoreInteractions(instanceClient);
    }

    @Test
    void refreshIfOlderThan_whenFreshFetchIsStillTooOld_throwsEmailRuntimeConfigException() {
        // arrange
        cache.put("current", runtimeConfig(2L));
        when(instanceClient.getRuntimeEmailConfig()).thenReturn(response(3L));
        // act
        EmailRuntimeConfigException exception = catchThrowableOfType(
                EmailRuntimeConfigException.class,
                () -> service.refreshIfOlderThan(5L));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.getEmailErrorCode()).isEqualTo(EmailErrorCode.EMAIL_RUNTIME_CONFIG_STALE);
        assertThat(exception.getMessage()).isEqualTo(EmailErrorCode.EMAIL_RUNTIME_CONFIG_STALE.getKey());
        assertThat(exception.getPublicMessage()).isEqualTo("Fetched stale email runtime config.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getPayload())
                .containsEntry("fetched_version", 3L)
                .containsEntry("required_scope_version", 5L);
        // verify
        verify(instanceClient).getRuntimeEmailConfig();
        verifyNoMoreInteractions(instanceClient);
    }

    @Test
    void getCurrentConfig_whenInstanceClientFails_throwsEmailRuntimeConfigException() {
        // arrange
        RuntimeException thrown = new RuntimeException("instance-service unavailable");
        when(instanceClient.getRuntimeEmailConfig()).thenThrow(thrown);
        // act
        EmailRuntimeConfigException exception = catchThrowableOfType(
                EmailRuntimeConfigException.class,
                () -> service.getCurrentConfig());
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.getEmailErrorCode()).isEqualTo(EmailErrorCode.EMAIL_RUNTIME_CONFIG_FETCH_FAILED);
        assertThat(exception.getMessage()).isEqualTo(EmailErrorCode.EMAIL_RUNTIME_CONFIG_FETCH_FAILED.getKey());
        assertThat(exception.getPublicMessage()).isEqualTo("Failed to fetch email runtime config.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getCause()).isSameAs(thrown);
        // verify
        verify(instanceClient).getRuntimeEmailConfig();
        verifyNoMoreInteractions(instanceClient);
    }

    @Test
    void evict_removesCachedValue() {
        // arrange
        when(instanceClient.getRuntimeEmailConfig()).thenReturn(response(1L), response(2L));
        EmailRuntimeConfig first = service.getCurrentConfig();
        // act
        service.evict();
        EmailRuntimeConfig second = service.getCurrentConfig();
        // assert
        assertThat(first.getVersion()).isEqualTo(1L);
        assertThat(second.getVersion()).isEqualTo(2L);
        assertThat(second).isNotSameAs(first);
        // verify
        verify(instanceClient, times(2)).getRuntimeEmailConfig();
        verifyNoMoreInteractions(instanceClient);
    }

    private static EmailRuntimeSecretConfigResponse response(long version) {
        return EmailRuntimeSecretConfigResponse.builder()
                .enabled(true)
                .host("smtp.example.com")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@example.com")
                .useTls(true)
                .useSsl(false)
                .version(version)
                .build();
    }

    private static EmailRuntimeConfig runtimeConfig(long version) {
        return EmailRuntimeConfig.builder()
                .enabled(true)
                .host("smtp.example.com")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@example.com")
                .useTls(true)
                .useSsl(false)
                .version(version)
                .build();
    }

}
