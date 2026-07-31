package com.syncturtle.services.user.support.fixture;

import java.time.Duration;
import java.util.UUID;

public final class PasswordResetFixtures {

    public static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID RESET_TOKEN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID OTHER_RESET_TOKEN_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    public static final String EMAIL = "lunasnow@marvel.com";
    public static final String EMAIL_WITH_WHITESPACE_AND_CASE = " lunasnow@MARVEL.com ";
    public static final String NORMALIZED_EMAIL = "lunasnow@marvel.com";
    public static final String INVALID_EMAIL = "not-an-email";

    public static final String UIDB64 = "encoded-user-id";
    public static final String RAW_TOKEN = "raw-password-reset-token";
    public static final String TOKEN_HASH = "hashed-password-reset-token";
    public static final String RESET_URL = "https://app.syncturtle.com/accounts/reset-password/"
            + UIDB64
            + "/"
            + RAW_TOKEN;
    public static final String VALID_PASSWORD = "Luna-Snow!2048";
    public static final String PASSWORD_HASH = "$2a$12$encoded-password";
    public static final String BLANK_PASSWORD = " ";
    public static final String SHORT_PASSWORD = "short";
    public static final String BLANK_TOKEN = " ";
    public static final int TOKEN_BYTES = 32;
    public static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    public static final int MAX_PASSWORD_LENGTH = 256;
    public static final int MAX_PRESENTED_TOKEN_LENGTH = 256;

    private PasswordResetFixtures() {
    }

    public static String maximumLengthPassword() {
        return "p".repeat(MAX_PASSWORD_LENGTH);
    }

    public static String oversizedPassword() {
        return "p".repeat(MAX_PASSWORD_LENGTH + 1);
    }

    public static String oversizedPresentedToken() {
        return "t".repeat(MAX_PRESENTED_TOKEN_LENGTH + 1);
    }

}
