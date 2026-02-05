package com.syncturtle.common.core.enums;

public enum AuthFlow {
    INSTANCE_ADMIN_SIGNUP(AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME),
    INSTANCE_ADMIN_SIGNIN(AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD),
    EMAIL_CHECK(AuthErrorCode.EMAIL_REQUIRED);

    private final AuthErrorCode defaultError;

    AuthFlow(AuthErrorCode defaultError) {
        this.defaultError = defaultError;
    }

    public AuthErrorCode getDefaultError() {
        return defaultError;
    }
}
