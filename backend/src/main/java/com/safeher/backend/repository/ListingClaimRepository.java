package com.safeher.backend.repository;

import com.safeher.backend.entity.ClaimStatus;
import com.safeher.backend.entity.Listing;
import com.safeher.backend.entity.ListingClaim;
import com.safeher.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingClaimRepository extends JpaRepository<ListingClaim, UUID> {

    List<ListingClaim> findByListingOrderByCreatedAtAsc(Listing listing);

    List<ListingClaim> findByClaimantOrderByCreatedAtDesc(User claimant);

    List<ListingClaim> findByListingAndClaimantOrderByCreatedAtDesc(
            Listing listing, User claimant);

    long countByListingAndStatus(Listing listing, ClaimStatus status);

    Optional<ListingClaim> findByIdAndClaimant(UUID id, User claimant);
}
