package com.syncturtle.services.instance.models.embedded;

import java.time.Instant;

import com.syncturtle.services.instance.payload.UpdateCheckMetadata;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateCheckInfo {

    @Column(name = "current_version", nullable = false)
    private String currentVersion;

    @Column(name = "latest_version", length = 50)
    private String latestVersion;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    public void apply(UpdateCheckMetadata m) {
        // TODO: refacto currentVersion
        this.currentVersion = m.getLatestVersion();
        this.latestVersion = m.getLatestVersion();
        this.lastCheckedAt = m.getLastCheckedAt();
    }

    public void touch(Instant now) {
        this.lastCheckedAt = now;
    }

    public static UpdateCheckInfo empty() {
        UpdateCheckInfo updateCheckInfo = new UpdateCheckInfo();
        return updateCheckInfo;
    }
}
