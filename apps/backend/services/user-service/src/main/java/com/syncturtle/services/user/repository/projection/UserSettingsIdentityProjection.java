package com.syncturtle.services.user.repository.projection;

import java.util.UUID;

public interface UserSettingsIdentityProjection {
    UUID getId();

    String getEmail();
}
