package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An offer of something to give, or a request for something needed.
 *
 * Note {@code userHandle} is the donation handle, not the posting handle. The
 * two are deliberately different pseudonyms for the same person: a request
 * reading "I cannot afford pads this month" is a disclosure of poverty, and
 * linking it to the same handle that writes about a domestic situation would
 * let anyone build a profile.
 */
@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** The donation-board pseudonym, not the board handle. */
    @Column(name = "user_handle", nullable = false, length = 50)
    private String userHandle;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_type_id", nullable = false)
    private ItemType itemType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Free text: "2 packs", "roughly 20 pads". */
    @Column(name = "quantity", length = 100)
    private String quantity;

    @Column(name = "area_name", length = 150)
    private String areaName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "approx_lat", precision = 9, scale = 6)
    private BigDecimal approxLat;

    @Column(name = "approx_lng", precision = 9, scale = 6)
    private BigDecimal approxLng;

    /**
     * When true, the description is withheld from the public listing and only
     * revealed to someone who makes contact. The item, city and count stay
     * visible, so the board can still show what is needed where.
     */
    @Column(name = "detail_hidden", nullable = false)
    @Builder.Default
    private boolean detailHidden = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "handled_by", nullable = false, length = 20)
    @Builder.Default
    private HandledBy handledBy = HandledBy.INDIVIDUAL;

    /** Set when handledBy is ORGANISATION. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.OPEN;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

        /**
     * The donor can split this between several people.
     *
     * Quantity is free text, so the platform cannot know that five packs
     * serves two people asking for two and three. Instead the donor says it
     * can be split, accepting no longer closes the listing, and she marks it
     * done when she runs out.
     */
    @Column(name = "can_split", nullable = false)
    @Builder.Default
    private boolean canSplit = false;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
