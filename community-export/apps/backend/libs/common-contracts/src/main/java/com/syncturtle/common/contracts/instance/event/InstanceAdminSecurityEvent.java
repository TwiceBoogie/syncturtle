package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceAdminSecurityEvent {

    public enum Type {
        ADMIN_GRANTED,
        ADMIN_ROLE_CHANGED,
        ADMIN_REVOKED,
        ADMIN_SESSION_REVOKED
    }

    private String eventId;
    private Instant occurredAt;

    private Type type;

    private UUID instanceId;
    private UUID userId;

    // security state owned by the instance-service
    private Long sessionVersion;
    private boolean active;
    @Builder.Default
    private List<String> roles = Collections.emptyList();

}
