package com.syncturtle.platform.services.instance.services.setup;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("setup")
@RequiredArgsConstructor
public class InstanceConfigurator {

    private final InstanceConfigurationResolver resolver;
    private final InstanceConfigurationWriter writer;

    @Transactional
    public void run() {
        resolver.ensureMandatorySecretsPresentOrThrow();

        // 1: seed managed keys (only inserts missing)
        writer.seedManagedKeysIfMissing();

        // 2: derived flags (only inserts if missing)
        writer.ensureDerivedFlagIfMissing(InstanceConfigurationKey.IS_GOOGLE_ENABLED,
                resolver.nonEmpty(InstanceConfigurationKey.GOOGLE_CLIENT_ID)
                        && resolver.nonEmpty(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET));

        writer.ensureDerivedFlagIfMissing(InstanceConfigurationKey.IS_GITHUB_ENABLED,
                resolver.nonEmpty(InstanceConfigurationKey.GITHUB_CLIENT_ID)
                        && resolver.nonEmpty(InstanceConfigurationKey.GITHUB_CLIENT_SECRET));

        writer.ensureDerivedFlagIfMissing(InstanceConfigurationKey.IS_GITLAB_ENABLED,
                resolver.nonEmpty(InstanceConfigurationKey.GITLAB_HOST) &&
                        resolver.nonEmpty(InstanceConfigurationKey.GITLAB_CLIENT_ID)
                        && resolver.nonEmpty(InstanceConfigurationKey.GITLAB_CLIENT_SECRET));

        writer.ensureDerivedFlagIfMissing(InstanceConfigurationKey.IS_INTERCOM_ENABLED,
                resolver.nonEmpty(InstanceConfigurationKey.INTERCOM_APP_ID));

        log.info("Instance configuration bootstrap complete.");
    }
}
