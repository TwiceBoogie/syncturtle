package com.syncturtle.common.security.provenance;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class RequestProvenanceSigningKey {

    private final String keyId;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public RequestProvenanceSigningKey(String keyId, PrivateKey privateKey, PublicKey publicKey) {
        Assert.hasText(keyId, "keyId is required");
        Assert.notNull(privateKey, "privateKey is required");
        Assert.notNull(publicKey, "publicKey is required");

        this.keyId = keyId;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

}
