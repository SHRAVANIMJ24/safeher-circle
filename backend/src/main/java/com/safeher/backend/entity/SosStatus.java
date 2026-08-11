package com.safeher.backend.entity;

public enum SosStatus {
    /** Raised and not yet resolved. */
    ACTIVE,
    /** The person confirmed they are all right. */
    SAFE,
    /** Triggered by accident and dismissed. */
    CANCELLED,
    /** Went unresolved long enough that we stopped treating it as live. */
    EXPIRED
}
