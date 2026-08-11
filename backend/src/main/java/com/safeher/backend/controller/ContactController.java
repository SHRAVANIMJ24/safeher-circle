package com.safeher.backend.controller;

import com.safeher.backend.dto.TrustedContactRequest;
import com.safeher.backend.dto.TrustedContactResponse;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Every route here is private to the signed-in account. */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<List<TrustedContactResponse>> list(
            @AuthenticationPrincipal User user) {
        requireUser(user);
        return ResponseEntity.ok(contactService.list(user));
    }

    @PostMapping
    public ResponseEntity<TrustedContactResponse> add(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TrustedContactRequest request) {
        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactService.add(user, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        contactService.remove(user, id);
        return ResponseEntity.noContent().build();
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
    }
}
