package com.safeher.backend.entity;

public enum ReportReason {
    SPAM,
    /** Abusive or hateful content directed at someone. */
    ABUSE,
    /** Appears fabricated, or a scam dressed as a request for help. */
    FAKE,
    /** Reveals someone's identity, address, workplace or phone number. */
    DOXXING,
    /** Describes intent to harm themselves or someone else. */
    SAFETY,
    OTHER
}
