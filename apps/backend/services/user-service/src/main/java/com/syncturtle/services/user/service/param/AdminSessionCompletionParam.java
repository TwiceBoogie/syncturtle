package com.syncturtle.services.user.service.param;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class AdminSessionCompletionParam {

    private final String completionCode;
    private final String preAuthBinding;
    private final String clientIp;
    private final String userAgent;

    @Builder
    private AdminSessionCompletionParam(
            String completionCode,
            String preAuthBinding,
            String clientIp,
            String userAgent) {
        Assert.hasText(completionCode, "completionCode is required");
        Assert.hasText(preAuthBinding, "preAuthBinding is required");

        this.completionCode = completionCode.trim();
        this.preAuthBinding = preAuthBinding.trim();
        this.clientIp = normalizeNullable(clientIp);
        this.userAgent = normalizeNullable(userAgent);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
