package com.syncturtle.services.file.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.file.kafka")
public final class FileKafkaProperties {

    private final String userAssetLinksGroupId;
    private final String workspaceAssetLinksGroupId;

    public FileKafkaProperties(String userAssetLinksGroupId, String workspaceAssetLinksGroupId) {
        Assert.hasText(userAssetLinksGroupId, "app.file.kafka.userAssetLinksGroupId is required");
        Assert.hasText(workspaceAssetLinksGroupId, "app.file.kafka.workspaceAssetLinksGroupId is required");

        this.userAssetLinksGroupId = userAssetLinksGroupId.trim();
        this.workspaceAssetLinksGroupId = workspaceAssetLinksGroupId.trim();
    }
}
