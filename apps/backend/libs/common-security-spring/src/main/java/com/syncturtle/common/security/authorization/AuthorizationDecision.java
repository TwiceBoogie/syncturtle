package com.syncturtle.common.security.authorization;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;

public final class AuthorizationDecision {

    public enum Type {
        ABSTAIN,
        ALLOW,
        DENY
    }

    private final Type type;
    private final AuthErrorCode errorCode;

    private AuthorizationDecision(Type type, AuthErrorCode errorCode) {
        Assert.notNull(type, "type is required");

        this.type = type;
        this.errorCode = errorCode;
    }

    public static AuthorizationDecision abstain() {
        return new AuthorizationDecision(Type.ABSTAIN, null);
    }

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(Type.ALLOW, null);
    }

    public static AuthorizationDecision deny(AuthErrorCode errorCode) {
        Assert.notNull(errorCode, "errorCode is required");

        return new AuthorizationDecision(Type.DENY, errorCode);
    }

    public boolean isAbstain() {
        return type == Type.ABSTAIN;
    }

    public boolean isAllow() {
        return type == Type.ALLOW;
    }

    public boolean isDenied() {
        return type == Type.DENY;
    }

    public AuthException toException() {
        if (!isDenied()) {
            throw new IllegalStateException("Only denied decisions can be converted to exceptions");
        }

        return AuthException.of(errorCode);
    }

}
