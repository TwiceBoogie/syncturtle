package com.syncturtle.common.contracts.auth.flow;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;

public enum AuthFlow {
    INSTANCE_ADMIN_SIGNUP(AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME),
    INSTANCE_ADMIN_SIGNIN(AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD),
    REGULAR_SIGN_UP(AuthErrorCode.REQUIRED_EMAIL_PASSWORD_SIGN_UP),
    REGULAR_SIGN_IN(AuthErrorCode.REQUIRED_EMAIL_PASSWORD_SIGN_IN),
    EMAIL_CHECK(AuthErrorCode.EMAIL_REQUIRED);

    private final AuthErrorCode defaultError;

    AuthFlow(AuthErrorCode defaultError) {
        this.defaultError = defaultError;
    }

    public AuthErrorCode getDefaultError() {
        return defaultError;
    }
}
