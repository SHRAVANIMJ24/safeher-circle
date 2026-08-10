package com.safeher.backend.entity;

public enum PostStatus {
    /** Held back by automated moderation, awaiting review. */
    PENDING,
    /** Visible on the board. */
    PUBLISHED,
    /** Reported by someone, still visible pending review. */
    FLAGGED,
    /** Taken down. Never returned by the public endpoints. */
    REMOVED
}
