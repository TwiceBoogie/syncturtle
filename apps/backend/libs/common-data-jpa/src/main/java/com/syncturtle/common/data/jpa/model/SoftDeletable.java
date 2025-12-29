package com.syncturtle.common.data.jpa.model;

import java.time.Instant;

/**
 * Marker interface so domain entities can adverties "I support soft delete".
 */
public interface SoftDeletable {
    Instant getDeletedAt();
}
