package com.syncturtle.common.web.error.mapper;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;

public final class AuthErrorPublicMessageMapper {

    private AuthErrorPublicMessageMapper() {
    }

    public static String toPublicMessage(AuthErrorCode errorCode) {
        if (errorCode == null) {
            return "Authentication failed.";
        }

        return switch (errorCode) {
            case INSTANCE_NOT_CONFIGURED -> "Instance is not configured.";
            case INVALID_EMAIL,
                    INVALID_EMAIL_SIGN_IN,
                    INVALID_EMAIL_SIGN_UP,
                    INVALID_ADMIN_EMAIL,
                    INVALID_EMAIL_MAGIC_SIGN_IN,
                    INVALID_EMAIL_MAGIC_SIGN_UP ->
                "Enter a valid email address.";

            case EMAIL_REQUIRED -> "Email is required.";
            case INVALID_PASSWORD,
                    INVALID_ADMIN_PASSWORD,
                    INVALID_NEW_PASSWORD ->
                "Enter a valid password.";

            case INVALID_PASSWORD_CONFIRM -> "Passwords do not match.";
            case ADMIN_ALREADY_EXIST -> "An instance admin already exists.";
            case ADMIN_USER_ALREADY_EXIST,
                    USER_ALREADY_EXIST ->
                "A user with this email already exists.";

            case SIGNUP_DISABLED -> "Sign up is disabled.";
            case MAGIC_LINK_LOGIN_DISABLED -> "Magic link login is disabled.";
            case PASSWORD_LOGIN_DISABLED -> "Password login is disabled.";
            case USER_ACCOUNT_DEACTIVATED,
                    ADMIN_USER_DEACTIVATED ->
                "This account is deactivated.";

            case RATE_LIMIT_EXCEEDED -> "Too many attempts. Try again later.";

            default -> "Authentication failed.";
        };
    }

}
