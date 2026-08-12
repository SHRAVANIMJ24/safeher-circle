package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered person. This is the only table holding identifying information —
 * everything public in the app refers to {@code anonHandle} instead, so a post
 * can never be traced back to an email address.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    /** Optional. Only needed once someone sets up emergency contacts. */
    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Public pseudonym, e.g. "quiet-lark-4471". Never changes. */
    @Column(name = "anon_handle", unique = true, nullable = false, length = 50)
    private String anonHandle;
    /**
     * A second pseudonym used only on the donation board.
     *
     * Generated on first use rather than at registration, so accounts that
     * never touch the board never get one. Kept separate from anonHandle
     * because "I cannot afford pads this month" is a disclosure of poverty,
     * and linking it to the handle that writes about a domestic situation
     * would let anyone assemble a profile.
     */
    @Column(name = "donation_handle", unique = true, length = 50)
    private String donationHandle;
    @Column(name = "display_city", length = 100)
    private String displayCity;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private boolean banned = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /** When they last opened their exchanges page. Drives the unread dot. */
    @Column(name = "exchanges_seen_at")
    private Instant exchangesSeenAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
