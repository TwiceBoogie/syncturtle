package com.syncturtle.services.instance.support.fixture;

import java.util.UUID;

import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupConfigResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;

public final class ResponseFixtures {

    private ResponseFixtures() {
    }

    public static InstanceSetupResponse inactiveSetupResponse() {
        return InstanceSetupResponse.builder()
                .isActivated(false)
                .isSetupDone(false)
                .build();
    }

    public static InstanceSetupConfigResponse setupConfigResponse() {
        return InstanceSetupConfigResponse.builder()
                .enableSignup(true)
                .workspaceCreationDisabled(false)
                .googleEnabled(false)
                .githubEnabled(true)
                .gitlabEnabled(false)
                .magicLoginEnabled(false)
                .emailPasswordEnabled(true)
                .githubAppName("syncturtle-dev")
                .posthogApiKey("posthog-test-key")
                .posthogHost("https://posthog.example.test")
                .smtpConfigured(false)
                .intercomEnabled(false)
                .intercomAppId("")
                .adminBaseUrl("http://localhost:3001")
                .appBaseUrl("http://localhost:3000")
                .instanceChangelogUrl("https://syncturtle.com")
                .build();
    }

    public static InstanceResponse instanceResponse(String name, long userCount) {
        return InstanceResponse.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .instanceName(name)
                .instanceId("instance-001")
                .currentVersion("0.0.1-test")
                .latestVersion("0.0.1-test")
                .namespace("dev")
                .telemetryEnabled(true)
                .supportRequired(false)
                .activated(true)
                .setupDone(true)
                .signupScreenVisited(false)
                .verified(false)
                .workspaceExist(false)
                .userCount(userCount)
                .createdAt(InstanceFixtures.REGISTERED_AT)
                .updatedAt(InstanceFixtures.UPDATED_AT)
                .build();
    }

}
