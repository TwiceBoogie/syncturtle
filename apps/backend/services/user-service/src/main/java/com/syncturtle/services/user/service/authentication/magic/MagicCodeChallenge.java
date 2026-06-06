package com.syncturtle.services.user.service.authentication.magic;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class MagicCodeChallenge {

    private final String key;
    private final String code;

    public MagicCodeChallenge(String key, String code) {
        Assert.hasText(key, "key is required");
        Assert.hasText(code, "code is required");

        this.key = key;
        this.code = code;
    }

}
