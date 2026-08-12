package com.safeher.backend.dto;

import com.safeher.backend.entity.ListingClaim;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String claimantHandle,
        String message,
        String status,
        Instant createdAt,

        // ---- handover ----
        String proposedPlace,
        String proposedTime,
        /** True when the viewer is the one who made the current proposal. */
        boolean proposedByMe,
        boolean handoverConfirmed,

        /** True when the viewer owns the listing, so the UI knows which side it is on. */
        boolean viewerIsOwner
) {
    public static ClaimResponse from(
            ListingClaim claim,
            String claimantHandle,
            UUID viewerId,
            boolean viewerIsOwner) {

        boolean proposedByMe = claim.getProposedBy() != null
                && viewerId != null
                && claim.getProposedBy().getId().equals(viewerId);

        return new ClaimResponse(
                claim.getId(),
                claim.getListing().getId(),
                claim.getListing().getTitle(),
                claimantHandle,
                claim.getMessage(),
                claim.getStatus().name(),
                claim.getCreatedAt(),
                claim.getProposedPlace(),
                claim.getProposedTime(),
                proposedByMe,
                claim.isHandoverConfirmed(),
                viewerIsOwner);
    }
}
