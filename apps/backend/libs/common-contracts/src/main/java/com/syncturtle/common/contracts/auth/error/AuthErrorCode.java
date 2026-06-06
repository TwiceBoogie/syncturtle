package com.syncturtle.common.contracts.auth.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

    INSTANCE_NOT_CONFIGURED(
            5000,
            "INSTANCE_NOT_CONFIGURED",
            503,
            "Instance is not configured."),

    INVALID_EMAIL(
            5005,
            "INVALID_EMAIL",
            400,
            "Email address is invalid."),

    EMAIL_REQUIRED(
            5010,
            "EMAIL_REQUIRED",
            400,
            "Email address is required."),

    GENERIC_INPUT_ERROR(
            5011,
            "GENERIC_INPUT_ERROR",
            400,
            "One or more fields are invalid."),

    INVALID_CSRF_TOKEN(
            5012,
            "INVALID_CSRF_TOKEN",
            403,
            "Security token is invalid or expired."),

    SIGNUP_DISABLED(
            5015,
            "SIGNUP_DISABLED",
            403,
            "Sign up is currently disabled."),

    MAGIC_LINK_LOGIN_DISABLED(
            5016,
            "MAGIC_LINK_LOGIN_DISABLED",
            403,
            "Magic link login is currently disabled."),

    PASSWORD_LOGIN_DISABLED(
            5018,
            "PASSWORD_LOGIN_DISABLED",
            403,
            "Password login is currently disabled."),

    USER_ACCOUNT_DEACTIVATED(
            5019,
            "USER_ACCOUNT_DEACTIVATED",
            403,
            "User account is deactivated."),

    INVALID_PASSWORD(
            5020,
            "INVALID_PASSWORD",
            400,
            "Password is invalid."),

    INVALID_PASSWORD_CONFIRM(
            5021,
            "INVALID_PASSWORD_CONFIRM",
            400,
            "Password confirmation does not match."),

    SMTP_NOT_CONFIGURED(
            5025,
            "SMTP_NOT_CONFIGURED",
            503,
            "Email delivery is not configured."),

    USER_ALREADY_EXISTS(
            5030,
            "USER_ALREADY_EXISTS",
            409,
            "User already exists."),

    AUTHENTICATION_FAILED_SIGN_UP(
            5035,
            "AUTHENTICATION_FAILED_SIGN_UP",
            401,
            "Could not complete sign up."),

    REQUIRED_EMAIL_PASSWORD_SIGN_UP(
            5040,
            "REQUIRED_EMAIL_PASSWORD_SIGN_UP",
            400,
            "Email and password are required."),

    INVALID_EMAIL_SIGN_UP(
            5045,
            "INVALID_EMAIL_SIGN_UP",
            400,
            "Email address is invalid."),

    INVALID_EMAIL_MAGIC_SIGN_UP(
            5050,
            "INVALID_EMAIL_MAGIC_SIGN_UP",
            400,
            "Email address is invalid."),

    MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED(
            5055,
            "MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED",
            400,
            "Email code is required."),

    EMAIL_PASSWORD_AUTHENTICATION_DISABLED(
            5056,
            "EMAIL_PASSWORD_AUTHENTICATION_DISABLED",
            403,
            "Email and password authentication is disabled."),

    USER_DOES_NOT_EXIST(
            5060,
            "USER_DOES_NOT_EXIST",
            401,
            "Authentication failed."),

    DEVICE_NOT_RECOGNIZED(
            5061,
            "DEVICE_NOT_RECOGNIZED",
            401,
            "Device verification is required."),

    EXPIRED_MAGIC_CODE_DEVICE(
            5062,
            "EXPIRED_MAGIC_CODE_DEVICE",
            401,
            "Device verification code has expired."),

    AUTHENTICATION_FAILED_SIGN_IN(
            5065,
            "AUTHENTICATION_FAILED_SIGN_IN",
            401,
            "Authentication failed."),

    REQUIRED_EMAIL_PASSWORD_SIGN_IN(
            5070,
            "REQUIRED_EMAIL_PASSWORD_SIGN_IN",
            400,
            "Email and password are required."),

    INVALID_EMAIL_SIGN_IN(
            5075,
            "INVALID_EMAIL_SIGN_IN",
            400,
            "Email address is invalid."),

    INVALID_EMAIL_MAGIC_SIGN_IN(
            5080,
            "INVALID_EMAIL_MAGIC_SIGN_IN",
            400,
            "Email address is invalid."),

    MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED(
            5085,
            "MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED",
            400,
            "Email code is required."),

    INVALID_MAGIC_CODE_SIGN_IN(
            5090,
            "INVALID_MAGIC_CODE_SIGN_IN",
            401,
            "Authentication failed."),

    INVALID_MAGIC_CODE_SIGN_UP(
            5092,
            "INVALID_MAGIC_CODE_SIGN_UP",
            401,
            "Authentication failed."),

    INVALID_MAGIC_CODE_DEVICE_VERIFICATION(
            5093,
            "INVALID_MAGIC_CODE_DEVICE_VERIFICATION",
            401,
            "Device verification failed."),

    EXPIRED_MAGIC_CODE_SIGN_IN(
            5095,
            "EXPIRED_MAGIC_CODE_SIGN_IN",
            401,
            "Authentication failed."),

    EXPIRED_MAGIC_CODE_SIGN_UP(
            5097,
            "EXPIRED_MAGIC_CODE_SIGN_UP",
            401,
            "Authentication failed."),

    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN(
            5100,
            "EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN",
            429,
            "Too many verification attempts."),

    EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_UP(
            5102,
            "EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_UP",
            429,
            "Too many verification attempts."),

    DEVICE_CODE_ATTEMPT_EXHAUSTED_VERIFICATION(
            5103,
            "DEVICE_CODE_ATTEMPT_EXHAUSTED_VERIFICATION",
            429,
            "Too many device verification attempts."),

    OAUTH_NOT_CONFIGURED(
            5104,
            "OAUTH_NOT_CONFIGURED",
            503,
            "OAuth is not configured."),

    GOOGLE_NOT_CONFIGURED(
            5105,
            "GOOGLE_NOT_CONFIGURED",
            503,
            "Google authentication is not configured."),

    GITHUB_NOT_CONFIGURED(
            5110,
            "GITHUB_NOT_CONFIGURED",
            503,
            "GitHub authentication is not configured."),

    GITLAB_NOT_CONFIGURED(
            5111,
            "GITLAB_NOT_CONFIGURED",
            503,
            "GitLab authentication is not configured."),

    GOOGLE_OAUTH_PROVIDER_ERROR(
            5115,
            "GOOGLE_OAUTH_PROVIDER_ERROR",
            502,
            "Google authentication provider failed."),

    GITHUB_OAUTH_PROVIDER_ERROR(
            5120,
            "GITHUB_OAUTH_PROVIDER_ERROR",
            502,
            "GitHub authentication provider failed."),

    GITLAB_OAUTH_PROVIDER_ERROR(
            5121,
            "GITLAB_OAUTH_PROVIDER_ERROR",
            502,
            "GitLab authentication provider failed."),

    INVALID_PASSWORD_TOKEN(
            5125,
            "INVALID_PASSWORD_TOKEN",
            401,
            "Password reset token is invalid."),

    EXPIRED_PASSWORD_TOKEN(
            5130,
            "EXPIRED_PASSWORD_TOKEN",
            401,
            "Password reset token has expired."),

    INCORRECT_OLD_PASSWORD(
            5135,
            "INCORRECT_OLD_PASSWORD",
            400,
            "Current password is incorrect."),

    MISSING_PASSWORD(
            5138,
            "MISSING_PASSWORD",
            400,
            "Password is required."),

    INVALID_NEW_PASSWORD(
            5140,
            "INVALID_NEW_PASSWORD",
            400,
            "New password is invalid."),

    PASSWORD_ALREADY_SET(
            5145,
            "PASSWORD_ALREADY_SET",
            409,
            "Password is already set."),

    ADMIN_ALREADY_EXISTS(
            5150,
            "ADMIN_ALREADY_EXISTS",
            409,
            "Admin already exists."),

    REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME(
            5155,
            "REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME",
            400,
            "Admin email, password, and first name are required."),

    INVALID_ADMIN_EMAIL(
            5160,
            "INVALID_ADMIN_EMAIL",
            400,
            "Admin email is invalid."),

    INVALID_ADMIN_PASSWORD(
            5165,
            "INVALID_ADMIN_PASSWORD",
            400,
            "Admin password is invalid."),

    REQUIRED_ADMIN_EMAIL_PASSWORD(
            5170,
            "REQUIRED_ADMIN_EMAIL_PASSWORD",
            400,
            "Admin email and password are required."),

    ADMIN_AUTHENTICATION_FAILED(
            5175,
            "ADMIN_AUTHENTICATION_FAILED",
            401,
            "Admin authentication failed."),

    ADMIN_USER_ALREADY_EXISTS(
            5180,
            "ADMIN_USER_ALREADY_EXISTS",
            409,
            "Admin user already exists."),

    ADMIN_USER_DOES_NOT_EXIST(
            5185,
            "ADMIN_USER_DOES_NOT_EXIST",
            404,
            "Admin user was not found."),

    ADMIN_USER_DEACTIVATED(
            5190,
            "ADMIN_USER_DEACTIVATED",
            403,
            "Admin user is deactivated."),

    RATE_LIMIT_EXCEEDED(
            5900,
            "RATE_LIMIT_EXCEEDED",
            429,
            "Too many requests."),

    AUTHENTICATION_FAILED(
            5999,
            "AUTHENTICATION_FAILED",
            401,
            "Authentication failed.");

    private static final Map<Integer, AuthErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(AuthErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    AuthErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
        this.code = code;
        this.key = key;
        this.httpStatusCode = httpStatusCode;
        this.publicMessage = publicMessage;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getPublicMessage() {
        return publicMessage;
    }

    public static AuthErrorCode fromCode(int code) {
        AuthErrorCode value = BY_CODE.get(code);
        if (value == null) {
            throw new IllegalArgumentException("Unknown AuthErrorCode: " + code);
        }

        return value;
    }

    public static AuthErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, AUTHENTICATION_FAILED);
    }

}