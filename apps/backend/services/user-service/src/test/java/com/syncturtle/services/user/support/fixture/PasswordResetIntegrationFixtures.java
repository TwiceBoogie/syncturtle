package com.syncturtle.services.user.support.fixture;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.services.user.model.PasswordResetToken;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.support.clock.TestClocks;

public final class PasswordResetIntegrationFixtures {

    public static final String ORIGINAL_PASSWORD_HASH = "$2a$12$original-password-hash-for-integration-tests";
    public static final String UPDATED_PASSWORD_HASH = "$2a$12$updated-password-hash-for-integration-tests";

    private PasswordResetIntegrationFixtures() {
        throw new AssertionError("PasswordResetIntegrationFixtures must not be instantiated");
    }

    public static User newUser(Clock clock) {
        Assert.notNull(clock, "clock is required");

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        UserCreateParam param = UserCreateParam.builder()
                .username("luna-" + suffix)
                .email("luna-" + suffix + "@marvel.com")
                .displayName("Luna Snow")
                .firstName("Luna")
                .lastName("Snow")
                .passwordHash(ORIGINAL_PASSWORD_HASH)
                .passwordAutoset(false)
                .userTimezone("America/Chicago")
                .initialLoginUserAgent("JUnit-test")
                .initialLoginIp("192.0.2.1")
                .initialLoginMedium("JUnit")
                .principalType(PrincipalType.HUMAN)
                .build();

        return User.create(param, clock);
    }

    public static PasswordResetToken newOutstandingToken(User user, String tokenHash) {
        return PasswordResetToken.issue(user, tokenHash, TestClocks.NOW.plus(PasswordResetFixtures.TOKEN_TTL));
    }

    public static PasswordResetToken newExpiredToken(User user, String tokenHash) {
        return PasswordResetToken.issue(user, tokenHash, TestClocks.NOW);
    }

    public static String uniqueRawToken(String prefix) {
        return uniqueValue(prefix);
    }

    public static String uniqueTokenHash(String prefix) {
        return uniqueValue(prefix + "-hash");
    }

    public static String uniqueMissingEmail() {
        return uniqueValue("missing-user") + "@example.com";
    }

    public static Instant priorTokenStateTime() {
        return TestClocks.ONE_MINUTE_BEFORE_NOW;
    }

    private static String uniqueValue(String prefix) {
        Assert.hasText(prefix, "prefix is required");

        return prefix + "-" + UUID.randomUUID();
    }

}
