package com.syncturtle.services.instance.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceAdminSessionResponse {
    boolean isAuthenticated;
    InstanceAdminMeResponse user;

    public static InstanceAdminSessionResponse anonymous() {
        return InstanceAdminSessionResponse.builder()
                .isAuthenticated(false)
                .user(null)
                .build();
    }

    public static InstanceAdminSessionResponse authenticated(InstanceAdminMeResponse user) {
        return InstanceAdminSessionResponse.builder()
                .isAuthenticated(true)
                .user(user)
                .build();
    }
}
