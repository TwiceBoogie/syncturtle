package com.syncturtle.services.user.support.factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.PasswordResetToken;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;

public final class PasswordResetMockFactory {

    private PasswordResetMockFactory() {
    }

    public static InstanceLite configuredInstance() {
        InstanceLite instance = mock(InstanceLite.class);

        when(instance.isSetupDone()).thenReturn(true);

        return instance;
    }

    public static InstanceLite unconfiguredInstance() {
        InstanceLite instance = mock(InstanceLite.class);

        when(instance.isSetupDone()).thenReturn(false);

        return instance;
    }

    public static UserAuthRuntimeSnapshot runtimeConfig(boolean smtpEnabled) {
        UserAuthRuntimeSnapshot configuration = mock(UserAuthRuntimeSnapshot.class);

        when(configuration.isSmtpEnabled()).thenReturn(smtpEnabled);

        return configuration;
    }

    public static User user() {
        return mock(User.class);
    }

    public static User user(UUID userId) {
        User user = user();

        when(user.getId()).thenReturn(userId);

        return user;
    }

    public static PasswordResetToken validResetToken() {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        /*
         * The remaining boolean methods return false by default:
         *
         * isExpired()
         * isConsumed()
         * isInvalidated()
         * isDeleted()
         */
        when(resetToken.matchesCurrentAuthVersion()).thenReturn(true);

        return resetToken;
    }

    public static PasswordResetToken validResetToken(UUID resetTokenId) {
        PasswordResetToken resetToken = validResetToken();

        when(resetToken.getId()).thenReturn(resetTokenId);

        return resetToken;
    }

    public static PasswordResetToken outstandingResetToken(UUID resetTokenId) {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        when(resetToken.getId()).thenReturn(resetTokenId);

        return resetToken;
    }

    public static PasswordResetToken outstandingResetToken() {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);
        return resetToken;
    }

    public static PasswordResetToken expiredResetToken(Instant now) {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        when(resetToken.isExpired(now)).thenReturn(true);

        return resetToken;
    }

    public static PasswordResetToken consumedResetToken() {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        when(resetToken.isConsumed()).thenReturn(true);

        return resetToken;
    }

    public static PasswordResetToken invalidatedResetToken() {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        when(resetToken.isInvalidated()).thenReturn(true);

        return resetToken;
    }

    public static PasswordResetToken deletedResetToken() {
        PasswordResetToken resetToken = mock(PasswordResetToken.class);

        when(resetToken.isDeleted()).thenReturn(true);

        return resetToken;
    }

    public static PasswordResetToken staleAuthVersionResetToken() {
        /*
         * matchesCurrentAuthVersion() returns false by default.
         */
        return mock(PasswordResetToken.class);
    }
}