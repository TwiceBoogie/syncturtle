package com.syncturtle.platform.services.instance.models.embedded;

import java.time.Instant;

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
public class ConfigInfo {

    @Column(name = "config_version", nullable = false)
    private long version;

    @Column(name = "config_last_checked_at")
    private Instant lastCheckedAt;

    public void initializeIfMissing(Instant now) {
        if (lastCheckedAt == null) {
            version = 0L;
            lastCheckedAt = now;
        }
    }

    public static ConfigInfo empty() {
        ConfigInfo cInfo = new ConfigInfo();
        return cInfo;
    }

    public void updateVersion() {
        version = version + 1;
        lastCheckedAt = Instant.now();
    }
}
