package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.enums.InstanceEdition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceEvent {
    public enum Type {
        INSTANCE_CREATED, INSTANCE_UPDATED, INSTANCE_SOFT_DELETED
    }

    private Type type;
    private UUID id;
    private InstanceEdition edition;
    private String slug;
    private long version;
    private String machineSignature;
    private String apiBaseUrl;
    private String vmHost;
    private Instant occurredAt;
    private boolean test;
}
