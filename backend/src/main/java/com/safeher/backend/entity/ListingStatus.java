package com.safeher.backend.entity;

public enum ListingStatus {
    OPEN,
    /** Someone has claimed it and the two are arranging a handover. */
    MATCHED,
    FULFILLED,
    EXPIRED
}
