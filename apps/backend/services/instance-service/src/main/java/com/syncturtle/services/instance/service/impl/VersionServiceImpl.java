package com.syncturtle.services.instance.service.impl;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;

import com.syncturtle.services.instance.client.version.GitHubReleaseClient;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.VersionService;
import com.syncturtle.services.instance.version.CurrentVersionResolver;
import com.syncturtle.services.instance.version.ReleaseVersion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("!setup")
@RequiredArgsConstructor
public class VersionServiceImpl implements VersionService {

    private final InstanceRepository repository;
    private final CurrentVersionResolver currentVersionResolver;
    private final GitHubReleaseClient releaseClient;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public void checkLatestRelease() {
        Assert.state(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Version check must not run inside an existing transaction");

        InstanceBinaryParam binary = currentVersionResolver.resolve(Instant.now(clock));
        Boolean instanceExists = transactionTemplate
                .execute(status -> refreshCurrentVersion(binary.getBinaryVersion()));

        if (!Boolean.TRUE.equals(instanceExists)) {
            return;
        }

        String latestTag;
        try {
            latestTag = releaseClient.latestTag();
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("Latest release check unavailable: {}", exception.getClass().getSimpleName());
            return;
        }

        if (latestTag != null && ReleaseVersion.parse(latestTag).isEmpty()) {
            log.warn("Ignoring malformed GitHub release tag");
            return;
        }

        Instant checkedAt = Instant.now(clock);

        transactionTemplate.executeWithoutResult(status -> recordLatestRelease(latestTag, checkedAt));
    }

    private boolean refreshCurrentVersion(String version) {
        Instance instance = repository
                .findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        if (instance == null) {
            return false;
        }

        if (!version.equals(instance.getUpdateCheck().getCurrentVersion())) {
            instance.updateCurrentVersion(version);
            repository.saveAndFlush(instance);
        }

        return true;
    }

    private void recordLatestRelease(String version, Instant checkedAt) {
        Instance instance = repository
                .findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        if (instance == null) {
            return;
        }

        instance.recordLatestRelease(version, checkedAt);
        repository.saveAndFlush(instance);
    }

}
