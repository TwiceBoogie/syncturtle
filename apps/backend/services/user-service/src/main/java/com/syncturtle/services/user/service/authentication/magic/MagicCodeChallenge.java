package com.syncturtle.services.user.service.authentication.magic;

import java.time.Duration;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class MagicCodeChallenge {

    private final String key;
    private final String token;
    private final Duration expiresIn;

    public MagicCodeChallenge(String key, String token, Duration expiresIn) {
        Assert.hasText(key, "key is required");
        Assert.hasText(token, "token is required");
        Assert.notNull(expiresIn, "expiresIn is required");

        this.key = key;
        this.token = token;
        this.expiresIn = expiresIn;
    }

}
