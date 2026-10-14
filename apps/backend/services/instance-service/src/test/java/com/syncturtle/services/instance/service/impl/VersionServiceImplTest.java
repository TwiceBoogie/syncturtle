package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.services.instance.client.version.GitHubReleaseClient;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationFlagsParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.model.param.InstanceRuntimeParam;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.version.CurrentVersionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("Version service")
class VersionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
    private static final Instant OLD_CHECK = NOW.minusSeconds(86_400);

    @Mock
    InstanceRepository repository;
    @Mock
    CurrentVersionResolver resolver;
    @Mock
    GitHubReleaseClient client;
    @Mock
    TransactionTemplate transactions;

    private VersionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VersionServiceImpl(
                repository, resolver, client, transactions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    class CheckLatestRelease {

        @Test
        void missingInstanceStopsBeforeGitHubLookup() {
            currentVersion("1.2.3");
            runFirstTransaction();
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());

            service.checkLatestRelease();

            verifyNoInteractions(client);
            verify(transactions, never()).executeWithoutResult(any());
        }

        @Test
        void commitsCurrentBeforeHttpAndLoadsFreshStateAfterHttp() {
            Instance firstLoad = instanceWithOldRelease();
            Instance freshSecondLoad = instanceWithOldRelease();
            freshSecondLoad.updateCurrentVersion("1.2.3");
            AtomicBoolean insideTransaction = new AtomicBoolean();
            List<String> order = new ArrayList<>();

            currentVersion("1.2.3");
            runTransactions(insideTransaction, order, true);
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(firstLoad))
                    .thenReturn(Optional.of(freshSecondLoad));
            when(repository.saveAndFlush(any(Instance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(client.latestTag()).thenAnswer(invocation -> {
                assertThat(insideTransaction.get()).isFalse();
                assertThat(order).containsExactly("first committed");
                assertThat(firstLoad.getUpdateCheck().getCurrentVersion()).isEqualTo("1.2.3");
                return "v1.3.0";
            });

            service.checkLatestRelease();

            assertThat(order).containsExactly("first committed", "second committed");
            assertThat(firstLoad).isNotSameAs(freshSecondLoad);
            assertThat(freshSecondLoad.getUpdateCheck().getCurrentVersion()).isEqualTo("1.2.3");
            assertThat(freshSecondLoad.getUpdateCheck().getLatestVersion()).isEqualTo("v1.3.0");
            assertThat(freshSecondLoad.getUpdateCheck().getLastCheckedAt()).isEqualTo(NOW);
            verify(repository).saveAndFlush(same(firstLoad));
            verify(repository).saveAndFlush(same(freshSecondLoad));
        }

        @Test
        void noReleaseClearsLatestAndRecordsSuccessfulCheckTime() {
            Instance instance = instanceWithOldRelease();
            currentVersion("0.0.1-test");
            runBothTransactions();
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(repository.saveAndFlush(instance)).thenReturn(instance);

            service.checkLatestRelease();

            assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("0.0.1-test");
            assertThat(instance.getUpdateCheck().getLatestVersion()).isNull();
            assertThat(instance.getUpdateCheck().getLastCheckedAt()).isEqualTo(NOW);
        }

        @Test
        void timeoutKeepsLastKnownReleaseWhileCurrentCanAdvance() {
            Instance instance = instanceWithOldRelease();
            currentVersion("1.2.3");
            runFirstTransaction();
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(repository.saveAndFlush(instance)).thenReturn(instance);
            when(client.latestTag()).thenThrow(new ResourceAccessException("timeout"));

            service.checkLatestRelease();

            assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("1.2.3");
            assertThat(instance.getUpdateCheck().getLatestVersion()).isEqualTo("v1.0.0");
            assertThat(instance.getUpdateCheck().getLastCheckedAt()).isEqualTo(OLD_CHECK);
            verify(transactions, never()).executeWithoutResult(any());
        }

        @Test
        void malformedTagKeepsLastKnownRelease() {
            Instance instance = instanceWithOldRelease();
            currentVersion("0.0.1-test");
            runFirstTransaction();
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(client.latestTag()).thenReturn("invalid-release");

            service.checkLatestRelease();

            assertThat(instance.getUpdateCheck().getLatestVersion()).isEqualTo("v1.0.0");
            assertThat(instance.getUpdateCheck().getLastCheckedAt()).isEqualTo(OLD_CHECK);
            verify(transactions, never()).executeWithoutResult(any());
        }

        @Test
        void databaseFailureStopsBeforeHttpAndPropagatesToSchedulerBoundary() {
            Instance instance = instanceWithOldRelease();
            currentVersion("1.2.3");
            runFirstTransaction();
            when(repository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(repository.saveAndFlush(instance)).thenThrow(new IllegalStateException("database unavailable"));

            assertThatThrownBy(() -> service.checkLatestRelease())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("database unavailable");
            verifyNoInteractions(client);
        }

        @Test
        void refusesAnAmbientTransactionBeforeAnyWork() {
            boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                assertThatThrownBy(() -> service.checkLatestRelease())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Version check must not run inside an existing transaction");
            } finally {
                TransactionSynchronizationManager.setActualTransactionActive(wasActive);
            }

            verifyNoInteractions(repository, resolver, client, transactions);
        }
    }

    private Instance instanceWithOldRelease() {
        InstanceRegistrationParam registration = InstanceRegistrationParam.builder()
                .instanceId("instance-version-test")
                .instanceName("SyncTurtle")
                .edition(InstanceEdition.COMMUNITY)
                .flags(InstanceRegistrationFlagsParam.builder()
                        .telemetryEnabled(false)
                        .supportRequired(false)
                        .test(true)
                        .build())
                .runtime(InstanceRuntimeParam.builder()
                        .domain("syncturtle.local")
                        .build())
                .binary(InstanceBinaryParam.of("0.0.1-test", OLD_CHECK))
                .registeredAt(OLD_CHECK)
                .build();
        Instance instance = Instance.register(registration);
        instance.recordLatestRelease("v1.0.0", OLD_CHECK);
        return instance;
    }

    private void currentVersion(String version) {
        when(resolver.resolve(NOW)).thenReturn(InstanceBinaryParam.of(version, NOW));
    }

    private void runFirstTransaction() {
        runTransactions(new AtomicBoolean(), new ArrayList<>(), false);
    }

    private void runBothTransactions() {
        runTransactions(new AtomicBoolean(), new ArrayList<>(), true);
    }

    private void runTransactions(AtomicBoolean insideTransaction, List<String> order, boolean includeSecond) {
        when(transactions.execute(any())).thenAnswer(invocation -> {
            assertThat(insideTransaction.get()).isFalse();
            insideTransaction.set(true);
            try {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mock(TransactionStatus.class));
            } finally {
                insideTransaction.set(false);
                order.add("first committed");
            }
        });
        if (!includeSecond) {
            return;
        }
        doAnswer(invocation -> {
            assertThat(insideTransaction.get()).isFalse();
            insideTransaction.set(true);
            try {
                Consumer<TransactionStatus> callback = invocation.getArgument(0);
                callback.accept(mock(TransactionStatus.class));
            } finally {
                insideTransaction.set(false);
                order.add("second committed");
            }
            return null;
        }).when(transactions).executeWithoutResult(any());
    }
}
