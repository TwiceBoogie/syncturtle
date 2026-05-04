package com.syncturtle.services.user.dto.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PassportTokenCommand {
    private final String userId;
    private final String instanceId;
    private final String sessionId;
    private final Long userAuthVersion;
    private final Long adminSessionVersion;
    private final List<String> roles;

    @Builder
    public PassportTokenCommand(
            String userId,
            String instanceId,
            String sessionId,
            Long userAuthVersion,
            Long adminSessionVersion,
            List<String> roles) {
        this.userId = userId;
        this.instanceId = instanceId;
        this.sessionId = sessionId;
        this.userAuthVersion = userAuthVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.roles = roles == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(roles));
    }
}
