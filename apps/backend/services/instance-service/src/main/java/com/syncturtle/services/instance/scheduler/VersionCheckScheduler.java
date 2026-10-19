package com.syncturtle.services.instance.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncturtle.services.instance.service.VersionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!setup")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.version-check", name = "enabled", havingValue = "true")
public class VersionCheckScheduler {

    private final VersionService versionService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT12H")
    public void checkLatestRelease() {
        try {
            versionService.checkLatestRelease();
        } catch (RuntimeException exception) {
            log.warn("Latest release check failed without affecting service availability", exception);
        }
    }

}
