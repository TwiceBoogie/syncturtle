package com.syncturtle.platform.services.workspace.repositories.query;

import java.time.Instant;
import java.util.UUID;

/**
 * Seek cursor for ORDER By createdAt DESC, id DESC
 */
public record WorkspaceCursor(Instant createdAt, UUID id) {

}
