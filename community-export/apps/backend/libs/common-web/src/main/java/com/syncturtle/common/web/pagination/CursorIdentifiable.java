package com.syncturtle.common.web.pagination;

import java.time.Instant;
import java.util.UUID;

public interface CursorIdentifiable {
    UUID getId();

    Instant getCreatedAt();
}
