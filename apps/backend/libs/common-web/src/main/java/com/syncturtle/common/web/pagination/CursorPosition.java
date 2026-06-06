package com.syncturtle.common.web.pagination;

import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class CursorPosition {

    private final Instant createdAt;
    private final UUID id;

    public CursorPosition(Instant createdAt, UUID id) {
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(id, "id is required");

        this.createdAt = createdAt;
        this.id = id;
    }

}
