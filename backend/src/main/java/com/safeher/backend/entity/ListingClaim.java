package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone responding to a listing.
 *
 * The handover fields sit here rather than in the message thread because
 * "where and when are we meeting" is the one thing both parties need to be able
 * to see at a glance, and it is exactly what gets lost in a chat.
 *
 * Either side may propose; the other confirms or counters. It is deliberately
 * not the donor who decides — the person receiving may not have the fare to
 * reach a place chosen for her, and has no say in whether it feels safe.
 */
@Entity
@Table(name = "listing_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.PENDING;

    // ---- handover arrangement ----

    @Column(name = "proposed_place", length = 300)
    private String proposedPlace;

    @Column(name = "proposed_time", length = 120)
    private String proposedTime;

    /** Who made the current proposal, so the other side is the one to confirm. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by_id")
    private User proposedBy;

    @Column(name = "proposed_at")
    private Instant proposedAt;

    @Column(name = "handover_confirmed", nullable = false)
    @Builder.Default
    private boolean handoverConfirmed = false;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
