package com.syncturtle.services.user.service.collaborator.session;

import java.util.regex.Pattern;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class RefreshSessionClientFingerprint {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final String deviceLabel;
    private final String clientBindingHash;

    public RefreshSessionClientFingerprint(String deviceLabel, String clientBindingHash) {
        Assert.hasText(clientBindingHash, "clientBindingHash is required");
        Assert.isTrue(SHA_256.matcher(clientBindingHash).matches(),
                "clientBindingHash must be lowercase SHA-256 hex");

        this.deviceLabel = deviceLabel;
        this.clientBindingHash = clientBindingHash;
    }

}
