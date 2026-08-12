package com.safeher.backend.dto;

import com.safeher.backend.entity.Listing;

import java.time.Instant;
import java.util.UUID;

/**
 * A listing as shown on the board.
 *
 * When {@code detailHidden} is set on a request, the description is replaced
 * rather than sent and hidden by the browser — a field the client is trusted
 * not to render is a field that leaks.
 */
public record ListingResponse(
        UUID id,
        String userHandle,
        String listingType,
        String itemSlug,
        String itemLabel,
        String title,
        String description,
        boolean detailHidden,
        /** The donor can give this to more than one person. */
        boolean canSplit,
        String quantity,
        String areaName,
        String city,
        String handledBy,
        String organisationName,
        String status,
        int claimCount,
        Instant createdAt
) {
    public static ListingResponse from(Listing listing, int claimCount) {
        boolean hide = listing.isDetailHidden();

        return new ListingResponse(
                listing.getId(),
                listing.getUserHandle(),
                listing.getListingType().name(),
                listing.getItemType().getSlug(),
                listing.getItemType().getLabel(),
                listing.getTitle(),
                hide ? null : listing.getDescription(),
                hide,
                listing.isCanSplit(),
                listing.getQuantity(),
                listing.getAreaName(),
                listing.getCity(),
                listing.getHandledBy().name(),
                listing.getOrganisation() == null
                        ? null : listing.getOrganisation().getName(),
                listing.getStatus().name(),
                claimCount,
                listing.getCreatedAt());
    }

    /** For the listing owner and anyone with an accepted claim. */
    public static ListingResponse full(Listing listing, int claimCount) {
        return new ListingResponse(
                listing.getId(),
                listing.getUserHandle(),
                listing.getListingType().name(),
                listing.getItemType().getSlug(),
                listing.getItemType().getLabel(),
                listing.getTitle(),
                listing.getDescription(),
                false,
                listing.isCanSplit(),
                listing.getQuantity(),
                listing.getAreaName(),
                listing.getCity(),
                listing.getHandledBy().name(),
                listing.getOrganisation() == null
                        ? null : listing.getOrganisation().getName(),
                listing.getStatus().name(),
                claimCount,
                listing.getCreatedAt());
    }
}
