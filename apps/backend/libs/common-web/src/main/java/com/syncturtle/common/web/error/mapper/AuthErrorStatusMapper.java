package com.syncturtle.common.web.error.mapper;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;

public final class AuthErrorStatusMapper {

    private AuthErrorStatusMapper() {
    }

    public static HttpStatus toHttpStatus(AuthErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.UNAUTHORIZED;
        }

        return switch (errorCode) {
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;

            case USER_DOES_NOT_EXIST,
                    AUTHENTICATION_FAILED,
                    AUTHENTICATION_FAILED_SIGN_IN,
                    AUTHENTICATION_FAILED_SIGN_UP,
                    ADMIN_AUTHENTICATION_FAILED,
                    INVALID_MAGIC_CODE_SIGN_IN,
                    INVALID_MAGIC_CODE_SIGN_UP,
                    INVALID_MAGIC_CODE_DEVICE_VERIFICATION,
                    EXPIRED_MAGIC_CODE_SIGN_IN,
                    EXPIRED_MAGIC_CODE_SIGN_UP,
                    EXPIRED_MAGIC_CODE_DEVICE,
                    INVALID_PASSWORD_TOKEN,
                    EXPIRED_PASSWORD_TOKEN ->
                HttpStatus.UNAUTHORIZED;

            case SIGNUP_DISABLED,
                    MAGIC_LINK_LOGIN_DISABLED,
                    PASSWORD_LOGIN_DISABLED,
                    EMAIL_PASSWORD_AUTHENTICATION_DISABLED,
                    USER_ACCOUNT_DEACTIVATED,
                    ADMIN_USER_DEACTIVATED ->
                HttpStatus.FORBIDDEN;

            case INSTANCE_NOT_CONFIGURED,
                    INVALID_EMAIL,
                    EMAIL_REQUIRED,
                    GENERIC_INPUT_ERROR,
                    INVALID_CSRF_TOKEN,
                    INVALID_PASSWORD,
                    SMTP_NOT_CONFIGURED,
                    INVALID_PASSWORD_CONFIRM,
                    USER_ALREADY_EXIST,
                    REQUIRED_EMAIL_PASSWORD_SIGN_UP,
                    INVALID_EMAIL_SIGN_UP,
                    INVALID_EMAIL_MAGIC_SIGN_UP,
                    MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED,
                    REQUIRED_EMAIL_PASSWORD_SIGN_IN,
                    INVALID_EMAIL_SIGN_IN,
                    INVALID_EMAIL_MAGIC_SIGN_IN,
                    MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED,
                    OAUTH_NOT_CONFIGURED,
                    GOOGLE_NOT_CONFIGURED,
                    GITHUB_NOT_CONFIGURED,
                    GITLAB_NOT_CONFIGURED,
                    GOOGLE_OAUTH_PROVIDER_ERROR,
                    GITHUB_OAUTH_PROVIDER_ERROR,
                    GITLAB_OAUTH_PROVIDER_ERROR,
                    INCORRECT_OLD_PASSWORD,
                    MISSING_PASSWORD,
                    INVALID_NEW_PASSWORD,
                    PASSWORD_ALREADY_SET,
                    ADMIN_ALREADY_EXIST,
                    REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME,
                    INVALID_ADMIN_EMAIL,
                    INVALID_ADMIN_PASSWORD,
                    REQUIRED_ADMIN_EMAIL_PASSWORD,
                    ADMIN_USER_ALREADY_EXIST,
                    ADMIN_USER_DOES_NOT_EXIST,
                    DEVICE_NOT_RECOGNIZED,
                    DEVICE_CODE_ATTEMPT_EXHAUSTED_VERIFICATION,
                    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN,
                    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_UP ->
                HttpStatus.BAD_REQUEST;

            default -> HttpStatus.BAD_REQUEST;
        };
    }

}
