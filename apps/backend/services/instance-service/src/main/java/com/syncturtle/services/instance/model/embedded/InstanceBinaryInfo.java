package com.syncturtle.services.instance.model.embedded;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceBinaryInfo {
    @Column(name = "binary_release_tag", nullable = false, length = 50)
    private String releaseTag;

    @Column(name = "binary_version", nullable = false, length = 50)
    private String version;

    @Column(name = "binary_commit", length = 40)
    private String commit;

    @Column(name = "binary_commit_short", length = 12)
    private String commitShort;

    @Column(name = "binary_built_at")
    private Instant builtAt;

    @Column(name = "binary_branch")
    private String branch;

    @Column(name = "binary_dirty")
    private Boolean dirty;

    @Column(name = "binary_build_id")
    private String buildId;

    @Column(name = "binary_ci")
    private String ci;

    @Column(name = "binary_platform")
    private String platform;

    public static InstanceBinaryInfo unknown() {
        InstanceBinaryInfo bi = new InstanceBinaryInfo();
        bi.releaseTag = "UNRELEASED";
        bi.version = "0.0.0.0";
        return bi;
    }
}
