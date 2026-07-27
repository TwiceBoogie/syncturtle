package com.syncturtle.services.user.service.authentication.oauth;

import org.springframework.util.Assert;

import com.syncturtle.services.user.model.User;

import lombok.Getter;

@Getter
public final class OAuthAuthenticationReceipt {

    private final User user;
    private final boolean createdUser;

    private OAuthAuthenticationReceipt(User user, boolean createdUser) {
        Assert.notNull(user, "user is required");

        this.user = user;
        this.createdUser = createdUser;
    }

    public static OAuthAuthenticationReceipt existingUser(User user) {
        return new OAuthAuthenticationReceipt(user, false);
    }

    public static OAuthAuthenticationReceipt createdUser(User user) {
        return new OAuthAuthenticationReceipt(user, true);
    }

}
