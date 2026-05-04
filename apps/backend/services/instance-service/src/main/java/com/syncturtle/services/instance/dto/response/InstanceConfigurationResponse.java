package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class InstanceConfigurationResponse {
    private UUID id;
    private InstanceConfigurationKey key;
    private String value;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdById;
    private UUID updatedById;
}
