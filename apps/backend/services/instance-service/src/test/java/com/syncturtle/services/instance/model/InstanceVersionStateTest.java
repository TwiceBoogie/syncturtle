package com.syncturtle.services.instance.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;

@DisplayName("Instance version state")
class InstanceVersionStateTest {

    @Test
    void setupInitializesCurrentWithoutInventingALatestRelease() {
        Instance instance = Instance.register(InstanceFixtures.registrationParam("SyncTurtle"));

        assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("0.0.1-test");
        assertThat(instance.getUpdateCheck().getLatestVersion()).isNull();
        assertThat(instance.getUpdateCheck().getLastCheckedAt()).isNull();
    }

    @Test
    void registrationRefreshUpdatesCurrentWithoutRewritingLatest() {
        InstanceRegistrationParam initial = InstanceFixtures.registrationParam("SyncTurtle");
        Instance instance = Instance.register(initial);
        Instant checkedAt = Instant.parse("2026-10-04T12:00:00Z");
        instance.recordLatestRelease("v1.0.0", checkedAt);
        InstanceRegistrationParam refresh = InstanceRegistrationParam.builder()
                .instanceId(initial.getInstanceId())
                .instanceName(initial.getInstanceName())
                .edition(initial.getEdition())
                .flags(initial.getFlags())
                .runtime(initial.getRuntime())
                .binary(InstanceBinaryParam.of("1.1.0", checkedAt))
                .registeredAt(checkedAt)
                .build();

        instance.refreshRegistration(refresh);

        assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("1.1.0");
        assertThat(instance.getUpdateCheck().getLatestVersion()).isEqualTo("v1.0.0");
        assertThat(instance.getUpdateCheck().getLastCheckedAt()).isEqualTo(checkedAt);
    }

    @Test
    void changingCurrentDoesNotOverwriteLastKnownRelease() {
        Instance instance = Instance.register(InstanceFixtures.registrationParam("SyncTurtle"));
        Instant checkedAt = Instant.parse("2026-10-04T12:00:00Z");
        instance.recordLatestRelease("v1.0.0", checkedAt);

        instance.updateCurrentVersion("1.1.0");

        assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("1.1.0");
        assertThat(instance.getUpdateCheck().getLatestVersion()).isEqualTo("v1.0.0");
        assertThat(instance.getUpdateCheck().getLastCheckedAt()).isEqualTo(checkedAt);
    }

}
