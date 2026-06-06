package com.syncturtle.services.instance.model.param;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class InstanceSetupCompletionParam {

    private static final int MAX_COMPANY_NAME_LENGTH = 100;

    private final String companyName;
    private final boolean telemetryEnabled;

    @Builder
    private InstanceSetupCompletionParam(
            String companyName,
            Boolean telemetryEnabled) {
        Assert.hasText(companyName, "companyName is required");
        Assert.notNull(telemetryEnabled, "telemetryEnabled is required");

        String normalizedCompanyName = companyName.trim();

        Assert.isTrue(normalizedCompanyName.length() <= MAX_COMPANY_NAME_LENGTH,
                "companyName must be " + MAX_COMPANY_NAME_LENGTH + " characters or fewer");

        this.companyName = normalizedCompanyName;
        this.telemetryEnabled = telemetryEnabled;
    }

}
