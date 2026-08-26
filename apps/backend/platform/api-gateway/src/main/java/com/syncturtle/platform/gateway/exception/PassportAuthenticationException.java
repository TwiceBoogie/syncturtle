package com.syncturtle.platform.gateway.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import lombok.Getter;

@Getter
public final class PassportAuthenticationException extends AuthenticationException {

    private final PassportAuthenticationFailureReason reason;

    public PassportAuthenticationException(PassportAuthenticationFailureReason reason) {
        this(reason, null);
    }

    public PassportAuthenticationException(PassportAuthenticationFailureReason reason, Throwable cause) {
        super("Passport authentication failed", cause);
        Assert.notNull(reason, "reason is required");

        this.reason = reason;
    }

}
