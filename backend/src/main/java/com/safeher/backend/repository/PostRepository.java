package com.safeher.backend.repository;

import com.safeher.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * JpaSpecificationExecutor is what lets the service compose optional filters
 * (category, city, search term) without writing one query per combination.
 */
public interface PostRepository
        extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
}
