package com.syncturtle.services.user.support.fixture;

import java.time.Instant;
import java.util.UUID;

public final class AuthenticationFixtures {

    public static final UUID INSTANCE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID CURRENT_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    public static final String EMAIL = "lunasnow@marvel.com";
    public static final String EMAIL_WITH_WHITESPACE_AND_CASE = " lunasnow@MARVEL.com ";
    public static final String NORMALIZED_EMAIL = "lunasnow@marvel.com";

    public static final String PASSWORD = "password-123";
    public static final String SHORT_PASSWORD = "short";
    public static final String MAGIC_CODE = "123456";

    public static final String SESSION_ID = "refresh-session-001";
    public static final String LOGOUT_CONTEXT = "USER";
    public static final String NEXT_PATH = "/workspace";
    public static final String SUCCESS_LOCATION = "/workspace";
    public static final String FAILURE_LOCATION = "/sign-in?code=5051&key=INSTANCE_NOT_CONFIGURED";
    public static final String SIGN_OUT_LOCATION = "/";

    public static final String CLIENT_IP = "127.0.0.1";
    public static final String USER_AGENT = "JUnit";

    public static final String ACCESS_TOKEN = "access-token";
    public static final String REFRESH_TOKEN = "refresh-token";

    public static final Instant ACCESS_ISSUED_AT = Instant.parse("2026-06-26T12:00:00Z");
    public static final Instant ACCESS_EXPIRES_AT = Instant.parse("2026-06-26T12:15:00Z");
    public static final Instant REFRESH_ISSUED_AT = Instant.parse("2026-06-26T12:00:00Z");
    public static final Instant REFRESH_EXPIRES_AT = Instant.parse("2026-07-26T12:00:00Z");

    private AuthenticationFixtures() {
    }

}
