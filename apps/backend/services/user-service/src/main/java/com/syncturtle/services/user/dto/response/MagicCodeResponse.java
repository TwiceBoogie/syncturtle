package com.syncturtle.services.user.dto.response;

import lombok.Getter;

@Getter
public final class MagicCodeResponse {
    private final String key;

    public MagicCodeResponse(String key) {
        this.key = key;
    }
}
