package com.safeher.backend.controller;

import com.safeher.backend.dto.CommentResponse;
import com.safeher.backend.dto.CreateCommentRequest;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** Reading replies is open, same as reading posts. */
    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> list(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.listForPost(postId));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> add(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {

        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.add(user, postId, request));
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        requireUser(user);
        commentService.remove(user, id);
        return ResponseEntity.noContent().build();
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to reply.");
        }
    }
}
