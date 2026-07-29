package com.syncturtle.services.instance.bootstrap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationWriter;
import com.syncturtle.services.instance.service.param.DerivedFlagParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("setup")
@RequiredArgsConstructor
public class InstanceConfigurator {

    private final InstanceConfigurationResolver configResolver;
    private final InstanceConfigurationWriter configWriter;

    @Transactional
    public void run() {
        configResolver.ensureMandatorySecretsPresentOrThrow();

        int managedSeeded = configWriter.seedManagedKeysIfMissing();
        int derivedSeeded = 0;

        derivedSeeded += seedDerivedFlag(DerivedFlagParam.builder()
                .key(InstanceConfigurationKey.IS_GOOGLE_ENABLED)
                .enabled(hasGoogleConfig())
                .build());

        derivedSeeded += seedDerivedFlag(DerivedFlagParam.builder()
                .key(InstanceConfigurationKey.IS_GITHUB_ENABLED)
                .enabled(hasGithubConfig())
                .build());

        derivedSeeded += seedDerivedFlag(DerivedFlagParam.builder()
                .key(InstanceConfigurationKey.IS_GITLAB_ENABLED)
                .enabled(hasGitlabConfig())
                .build());

        derivedSeeded += seedDerivedFlag(DerivedFlagParam.builder()
                .key(InstanceConfigurationKey.IS_INTERCOM_ENABLED)
                .enabled(hasIntercomConfig())
                .build());

        log.info(
                "Instance configuration bootstrap complete. managedSeeded={} derivedSeeded={}",
                managedSeeded,
                derivedSeeded);
    }

    private int seedDerivedFlag(DerivedFlagParam param) {
        return configWriter.ensureDerivedFlagIfMissing(param) ? 1 : 0;
    }

    private boolean hasGoogleConfig() {
        return hasAll(
                InstanceConfigurationKey.GOOGLE_CLIENT_ID,
                InstanceConfigurationKey.GOOGLE_CLIENT_SECRET);
    }

    private boolean hasGithubConfig() {
        return hasAll(
                InstanceConfigurationKey.GITHUB_CLIENT_ID,
                InstanceConfigurationKey.GITHUB_CLIENT_SECRET);
    }

    private boolean hasGitlabConfig() {
        return hasAll(
                InstanceConfigurationKey.GITLAB_HOST,
                InstanceConfigurationKey.GITLAB_CLIENT_ID,
                InstanceConfigurationKey.GITLAB_CLIENT_SECRET);
    }

    private boolean hasIntercomConfig() {
        return hasAll(InstanceConfigurationKey.INTERCOM_APP_ID);
    }

    private boolean hasAll(InstanceConfigurationKey... keys) {
        for (InstanceConfigurationKey key : keys) {
            if (!configResolver.nonEmpty(key)) {
                return false;
            }
        }

        return true;
    }

}
