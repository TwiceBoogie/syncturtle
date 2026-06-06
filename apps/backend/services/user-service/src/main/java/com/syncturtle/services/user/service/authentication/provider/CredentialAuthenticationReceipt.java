package com.syncturtle.services.user.service.authentication.provider;

import org.springframework.util.Assert;

import com.syncturtle.services.user.model.User;

import lombok.Getter;

@Getter
public final class CredentialAuthenticationReceipt {

    private final User user;
    private final boolean createdUser;

    private CredentialAuthenticationReceipt(User user, boolean createdUser) {
        Assert.notNull(user, "user is required");

        this.user = user;
        this.createdUser = createdUser;
    }

    public static CredentialAuthenticationReceipt existingUser(User user) {
        return new CredentialAuthenticationReceipt(user, false);
    }

    public static CredentialAuthenticationReceipt createdUser(User user) {
        return new CredentialAuthenticationReceipt(user, true);
    }

}
