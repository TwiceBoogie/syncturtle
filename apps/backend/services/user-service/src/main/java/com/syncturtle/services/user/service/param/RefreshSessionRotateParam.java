package com.syncturtle.services.user.service.param;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RefreshSessionRotateParam {

    private final String sessionId;
    private final String userId;
    private final String instanceId;
    private final String email;
    private final List<String> roles;
    private final Long authVersion;
    private final Long adminSessionVersion;
    private final String ipAddress;
    private final String userAgent;

    @Builder
    public RefreshSessionRotateParam(
            String sessionId,
            String userId,
            String instanceId,
            String email,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String ipAddress,
            String userAgent) {

        this.sessionId = sessionId;
        this.userId = userId;
        this.instanceId = instanceId;
        this.email = email;
        this.roles = roles == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(roles));
        this.authVersion = authVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
