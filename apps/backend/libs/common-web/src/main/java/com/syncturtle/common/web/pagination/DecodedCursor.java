package com.syncturtle.common.web.pagination;

import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class DecodedCursor {

    private final UUID id;
    private final Instant createdAt;

    public DecodedCursor(UUID id, Instant createdAt) {
        Assert.notNull(id, "id is required");
        Assert.notNull(createdAt, "createdAt is required");

        this.id = id;
        this.createdAt = createdAt;
    }

    public CursorPosition toPosition() {
        return new CursorPosition(createdAt, id);
    }

}
