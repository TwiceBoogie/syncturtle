package com.syncturtle.services.instance.model.param;

import java.time.Instant;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class UpdateCheckParam {

    private final String latestVersion;
    private final Instant lastCheckedAt;

    private UpdateCheckParam(String latestVersion, Instant lastCheckedAt) {
        Assert.hasText(latestVersion, "latestVersion is required");
        Assert.notNull(lastCheckedAt, "lastCheckAt is required");

        this.latestVersion = latestVersion.trim();
        this.lastCheckedAt = lastCheckedAt;
    }

    public static UpdateCheckParam initial(String version, Instant checkedAt) {
        return new UpdateCheckParam(version, checkedAt);
    }

    public static UpdateCheckParam checked(String latestVersion, Instant checkedAt) {
        return new UpdateCheckParam(latestVersion, checkedAt);
    }

}
