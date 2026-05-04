package com.syncturtle.services.user.dto.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateRefreshSessionCommand {

    private final String userId;
    private final String instanceId;
    private final String email;
    private final List<String> roles;
    private final Long authVersion;
    private final Long adminSessionVersion;
    private final String ipAddress;
    private final String userAgent;

    @Builder
    public CreateRefreshSessionCommand(
            String userId,
            String instanceId,
            String email,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String ipAddress,
            String userAgent) {

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
