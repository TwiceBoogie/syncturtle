package com.syncturtle.services.user.service.authentication.magic;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class MagicCodeChallenge {

    private final String key;
    private final String token;

    public MagicCodeChallenge(String key, String token) {
        Assert.hasText(key, "key is required");
        Assert.hasText(token, "token is required");

        this.key = key;
        this.token = token;
    }

}
