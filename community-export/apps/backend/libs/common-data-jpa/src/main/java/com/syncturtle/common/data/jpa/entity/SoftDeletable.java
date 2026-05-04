package com.syncturtle.common.data.jpa.entity;

import java.time.Instant;

/**
 * Marker interface so domain entities can adverties "I support soft delete".
 */
public interface SoftDeletable {
    Instant getDeletedAt();
}
