package com.syncturtle.services.user.testsupport.factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.syncturtle.services.user.model.Instance;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.projection.UserPasswordAutosetProjection;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures;

public final class AuthenticationMockFactory {

    private AuthenticationMockFactory() {
    }

    public static Instance configuredInstanceWithId() {
        Instance instance = mock(Instance.class);

        when(instance.getId()).thenReturn(AuthenticationFixtures.INSTANCE_ID);
        when(instance.isSetupDone()).thenReturn(true);

        return instance;
    }

    public static Instance configuredInstance() {
        Instance instance = mock(Instance.class);

        when(instance.isSetupDone()).thenReturn(true);

        return instance;
    }

    public static Instance unconfiguredInstance() {
        Instance instance = mock(Instance.class);

        when(instance.isSetupDone()).thenReturn(false);

        return instance;
    }

    public static User user() {
        return mock(User.class);
    }

    public static User userWithAutosetPassword() {
        User user = mock(User.class);

        when(user.isPasswordAutoset()).thenReturn(true);

        return user;
    }

    public static User userWithPasswordAlreadySet() {
        User user = mock(User.class);

        when(user.isPasswordAutoset()).thenReturn(false);

        return user;
    }

    public static UserPasswordAutosetProjection passwordAutosetProjection(boolean passwordAutoset) {
        UserPasswordAutosetProjection projection = mock(UserPasswordAutosetProjection.class);

        when(projection.isPasswordAutoset()).thenReturn(passwordAutoset);

        return projection;
    }

    public static UserAuthRuntimeSnapshot authConfig(boolean magicLinkEnabled, boolean smtpEnabled,
            boolean signupEnabled) {
        UserAuthRuntimeSnapshot config = mock(UserAuthRuntimeSnapshot.class);

        when(config.isMagicLinkEnabled()).thenReturn(magicLinkEnabled);
        when(config.isSmtpEnabled()).thenReturn(smtpEnabled);
        when(config.isSignupEnabled()).thenReturn(signupEnabled);

        return config;
    }

    public static UserAuthRuntimeSnapshot authConfig(boolean magicLinkEnabled, boolean smtpEnabled) {
        UserAuthRuntimeSnapshot config = mock(UserAuthRuntimeSnapshot.class);

        when(config.isMagicLinkEnabled()).thenReturn(magicLinkEnabled);
        when(config.isSmtpEnabled()).thenReturn(smtpEnabled);

        return config;
    }

}
