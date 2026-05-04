package com.syncturtle.services.workspace.payload;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursorPayload {
    private UUID id; // last id
    private Instant createdAt;
}
