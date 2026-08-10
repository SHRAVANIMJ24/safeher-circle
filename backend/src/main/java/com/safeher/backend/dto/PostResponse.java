package com.safeher.backend.dto;

import com.safeher.backend.entity.Post;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * What the board actually sends to the browser. There is no author id here on
 * purpose — the handle is the only identity that leaves the server.
 */
public record PostResponse(
        UUID id,
        String authorHandle,
        String categorySlug,
        String categoryLabel,
        String title,
        String body,
        String areaName,
        String city,
        String state,
        BigDecimal approxLat,
        BigDecimal approxLng,
        int upvoteCount,
        int commentCount,
        String status,
        Instant createdAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthorHandle(),
                post.getCategory().getSlug(),
                post.getCategory().getLabel(),
                post.getTitle(),
                post.getBody(),
                post.getAreaName(),
                post.getCity(),
                post.getState(),
                post.getApproxLat(),
                post.getApproxLng(),
                post.getUpvoteCount(),
                post.getCommentCount(),
                post.getStatus().name(),
                post.getCreatedAt());
    }
}
