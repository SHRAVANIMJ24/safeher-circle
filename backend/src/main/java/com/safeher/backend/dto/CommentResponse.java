package com.safeher.backend.dto;

import com.safeher.backend.entity.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A comment and the replies underneath it.
 *
 * The tree is assembled server-side so the browser gets it ready to render
 * rather than having to stitch a flat list together itself.
 */
public record CommentResponse(
        UUID id,
        String authorHandle,
        String body,
        boolean isAuthorOfPost,
        Instant createdAt,
        List<CommentResponse> replies
) {
    public static CommentResponse from(Comment comment, String postAuthorHandle) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthorHandle(),
                comment.getBody(),
                comment.getAuthorHandle().equals(postAuthorHandle),
                comment.getCreatedAt(),
                new ArrayList<>());
    }
}
