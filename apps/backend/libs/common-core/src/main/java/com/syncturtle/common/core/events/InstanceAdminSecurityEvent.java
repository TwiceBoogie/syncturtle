package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private List<String> roles;

}
