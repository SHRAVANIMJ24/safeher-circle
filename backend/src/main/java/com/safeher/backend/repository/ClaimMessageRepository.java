package com.safeher.backend.repository;

import com.safeher.backend.entity.ClaimMessage;
import com.safeher.backend.entity.ListingClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClaimMessageRepository extends JpaRepository<ClaimMessage, UUID> {

    List<ClaimMessage> findByClaimOrderByCreatedAtAsc(ListingClaim claim);
    long countByClaimInAndCreatedAtAfter(
            List<ListingClaim> claims, Instant after);
}
