package com.safeher.backend.entity;

public enum ClaimStatus {
    /** Sent, waiting for the listing owner. */
    PENDING,
    /** The owner picked this one. */
    ACCEPTED,
    /** The owner said no. The claimant may try again later. */
    DECLINED,
    /** The claimant changed their mind. Puts the listing back on the board. */
    WITHDRAWN,
    /** The handover happened. */
    COMPLETED
}
