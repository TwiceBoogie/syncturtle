package com.syncturtle.services.user.service.collaborator.authentication.password;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.web.url.PublicUrlResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordResetUrlBuilder {

    private static final String RESET_PASSWORD_PATH = "/accounts/reset-password/";

    private final PublicUrlResolver publicUrlResolver;

    public String build(String uidb64, String rawToken) {
        Assert.hasText(uidb64, "uidb64 is required");
        Assert.hasText(rawToken, "rawToken is required");

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("uidb64", uidb64);
        queryParams.put("token", rawToken);

        return publicUrlResolver.userAppWithQuery(RESET_PASSWORD_PATH, queryParams);
    }

}
