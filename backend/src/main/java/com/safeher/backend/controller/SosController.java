package com.safeher.backend.controller;

import com.safeher.backend.dto.SosAlertResponse;
import com.safeher.backend.dto.TriggerSosRequest;
import com.safeher.backend.entity.SosStatus;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @PostMapping
    public ResponseEntity<SosAlertResponse> trigger(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TriggerSosRequest request) {
        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sosService.trigger(user, request));
    }

    /** The "I'm safe" button. */
    @PostMapping("/{id}/safe")
    public ResponseEntity<SosAlertResponse> markSafe(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(sosService.resolve(user, id, SosStatus.SAFE));
    }

    /** For a trigger that was clearly an accident. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<SosAlertResponse> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(sosService.resolve(user, id, SosStatus.CANCELLED));
    }

    @GetMapping("/active")
    public ResponseEntity<SosAlertResponse> active(@AuthenticationPrincipal User user) {
        requireUser(user);
        return ResponseEntity.ok(sosService.active(user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SosAlertResponse>> history(
            @AuthenticationPrincipal User user) {
        requireUser(user);
        return ResponseEntity.ok(sosService.history(user));
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
    }
}
