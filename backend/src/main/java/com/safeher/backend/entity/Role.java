package com.safeher.backend.entity;

public enum Role {
    /** Anyone who signed up. */
    USER,
    /** Can review reports and hide content. */
    MODERATOR,
    /** Full access, including moderator management. */
    ADMIN,
    /** A verified organisation account. */
    NGO
}
