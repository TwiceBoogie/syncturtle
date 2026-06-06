package com.syncturtle.services.instance.model.embedded;

import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.services.instance.model.param.UpdateCheckParam;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateCheckInfoEmbed {

    @Column(name = "current_version", nullable = false, length = 50)
    private String currentVersion;

    @Column(name = "latest_version", length = 50)
    private String latestVersion;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    public void apply(UpdateCheckParam param) {
        Assert.notNull(param, "update check param is required");

        if (currentVersion == null) {
            currentVersion = param.getLatestVersion();
        }

        latestVersion = param.getLatestVersion();
        lastCheckedAt = param.getLastCheckedAt();
    }

    public void touch(Instant now) {
        Assert.notNull(now, "now is required");

        lastCheckedAt = now;
    }

    public static UpdateCheckInfoEmbed empty() {
        return new UpdateCheckInfoEmbed();
    }
}
