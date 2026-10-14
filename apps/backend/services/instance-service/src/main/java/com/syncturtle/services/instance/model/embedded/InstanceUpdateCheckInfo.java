package com.syncturtle.services.instance.model.embedded;

import java.time.Instant;

import org.springframework.util.Assert;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceUpdateCheckInfo {

    @Column(name = "current_version", nullable = false, length = 50)
    private String currentVersion;

    @Column(name = "latest_version", length = 50)
    private String latestVersion;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    public void updateCurrentVersion(String version) {
        Assert.hasText(version, "current version is required");

        currentVersion = version.trim();
    }

    public void recordLatestRelease(String version, Instant checkedAt) {
        Assert.notNull(checkedAt, "checkedAt is required");

        if (version != null) {
            Assert.hasText(version, "latest version must not be blank");

            latestVersion = version.trim();
        } else {
            latestVersion = null;
        }

        lastCheckedAt = checkedAt;
    }

    public static InstanceUpdateCheckInfo empty() {
        return new InstanceUpdateCheckInfo();
    }
}
