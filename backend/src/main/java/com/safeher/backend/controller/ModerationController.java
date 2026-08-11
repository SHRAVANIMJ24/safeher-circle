package com.safeher.backend.controller;

import com.safeher.backend.dto.ModerationDecision;
import com.safeher.backend.dto.PageResponse;
import com.safeher.backend.dto.ReportResponse;
import com.safeher.backend.entity.User;
import com.safeher.backend.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Access is restricted in SecurityConfig to MODERATOR and ADMIN, so these
 * methods do not repeat the role check.
 */
@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<ReportResponse>> queue(
            @RequestParam(required = false, defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(moderationService.queue(status, page, size));
    }

    @PostMapping("/reports/{id}")
    public ResponseEntity<ReportResponse> decide(
            @AuthenticationPrincipal User moderator,
            @PathVariable UUID id,
            @Valid @RequestBody ModerationDecision decision) {

        return ResponseEntity.ok(moderationService.decide(moderator, id, decision));
    }
}
