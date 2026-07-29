package com.syncturtle.services.instance.mapper;

import org.springframework.stereotype.Component;

import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.model.Instance;

@Component
public final class InstanceApiMapper {

    public InstanceSetupResponse toInactiveInstanceResponse() {
        return InstanceSetupResponse.builder()
                .isActivated(false)
                .isSetupDone(false)
                .build();
    }

    public InstanceResponse toInstanceResponse(Instance instance, long userCount, boolean workspaceExist) {
        return InstanceResponse.builder()
                .id(instance.getId())
                .instanceName(instance.getInstanceName())
                .whitelistEmails(instance.getWhitelistEmails())
                .licenseKey(null)
                .instanceId(instance.getInstanceId())
                .currentVersion(instance.getUpdateCheck().getCurrentVersion())
                .latestVersion(instance.getUpdateCheck().getLatestVersion())
                .lastCheckedAt(instance.getUpdateCheck().getLastCheckedAt())
                .namespace(instance.getRuntime().getNamespace())
                .telemetryEnabled(instance.isTelemetryEnabled())
                .supportRequired(instance.isSupportRequired())
                .activated(true)
                .setupDone(instance.isSetupDone())
                .signupScreenVisited(instance.isSignupScreenVisited())
                .verified(instance.isVerified())
                .workspaceExist(workspaceExist)
                .userCount(userCount)
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .createdBy(instance.getCreatedById())
                .updatedBy(instance.getUpdatedById())
                .build();
    }

}
