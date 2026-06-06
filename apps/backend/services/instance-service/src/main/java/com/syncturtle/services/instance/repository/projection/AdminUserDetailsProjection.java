package com.syncturtle.services.instance.repository.projection;

import java.util.UUID;

public interface AdminUserDetailsProjection extends AdminUserDetailLiteProjection {
    String getUsername();

    String getUserTimezone();

    UUID getCoverImageAssetId();

    boolean isEmailVerified();

    boolean isPasswordAutoset();

    String getLastLoginMedium();
}
