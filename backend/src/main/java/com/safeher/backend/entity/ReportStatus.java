package com.safeher.backend.entity;

public enum ReportStatus {
    /** Waiting for a moderator. */
    OPEN,
    /** A moderator has picked it up. */
    REVIEWING,
    /** Reviewed, and something was done about it. */
    ACTIONED,
    /** Reviewed, and the content was left alone. */
    DISMISSED
}
