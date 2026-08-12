package com.safeher.backend.repository;

import com.safeher.backend.entity.Listing;
import com.safeher.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ListingRepository
        extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByUserOrderByCreatedAtDesc(User user);
}
