package com.safeher.backend.controller;

import com.safeher.backend.dto.CreatePostRequest;
import com.safeher.backend.dto.PageResponse;
import com.safeher.backend.dto.PostResponse;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** Reading the board is open to everyone, signed in or not. */
    @GetMapping
    public ResponseEntity<PageResponse<PostResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                postService.list(category, city, search, sort, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.getById(id));
    }

    /** Posting requires an account, so that bans and rate limits can apply. */
    @PostMapping
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePostRequest request) {

        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to post.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(user, request));
    }
}
