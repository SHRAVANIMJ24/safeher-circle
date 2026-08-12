package com.safeher.backend.controller;

import com.safeher.backend.dto.*;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    // ---------- listings ----------

    /** Browsing is open. Someone should be able to see what is needed nearby
     *  before deciding whether to make an account. */
    @GetMapping("/listings")
    public ResponseEntity<PageResponse<ListingResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String item,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(donationService.list(type, item, city, page, size));
    }

    /**
     * Declared before /listings/{id} so that "mine" is matched as a literal
     * path rather than parsed as a UUID.
     */
    @GetMapping("/listings/mine")
    public ResponseEntity<List<ListingResponse>> mine(@AuthenticationPrincipal User user) {
        requireUser(user);
        return ResponseEntity.ok(donationService.mine(user));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> getOne(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(donationService.getById(id, user));
    }

    @GetMapping("/items")
    public ResponseEntity<List<ItemTypeResponse>> itemTypes() {
        return ResponseEntity.ok(donationService.itemTypes());
    }

    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateListingRequest request) {
        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.create(user, request));
    }

    /** How a splittable listing is closed: the donor says she has run out. */
    @PostMapping("/listings/{id}/fulfilled")
    public ResponseEntity<ListingResponse> markFulfilled(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.markFulfilled(user, id));
    }

    // ---------- responses to a listing ----------

    @PostMapping("/listings/{id}/claim")
    public ResponseEntity<ClaimResponse> claim(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody CreateClaimRequest request) {
        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.claim(user, id, request));
    }

    @GetMapping("/listings/{id}/claims")
    public ResponseEntity<List<ClaimResponse>> claimsOnListing(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.claimsOnMyListings(user, id));
    }

    @GetMapping("/claims/mine")
    public ResponseEntity<List<ClaimResponse>> myClaims(@AuthenticationPrincipal User user) {
        requireUser(user);
        return ResponseEntity.ok(donationService.myClaims(user));
    }

    @PostMapping("/claims/{id}/accept")
    public ResponseEntity<ClaimResponse> accept(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.respondToClaim(user, id, true));
    }

    @PostMapping("/claims/{id}/decline")
    public ResponseEntity<ClaimResponse> decline(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.respondToClaim(user, id, false));
    }

    /** The claimant backing out, which puts the listing back on the board. */
    @PostMapping("/claims/{id}/withdraw")
    public ResponseEntity<ClaimResponse> withdraw(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.withdrawClaim(user, id));
    }

    // ---------- arranging the handover ----------

    /** Either side may propose a place and time. */
    @PostMapping("/claims/{id}/handover")
    public ResponseEntity<ClaimResponse> proposeHandover(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody HandoverProposal proposal) {
        requireUser(user);
        return ResponseEntity.ok(donationService.proposeHandover(user, id, proposal));
    }

    /** Only the side that did not propose may confirm. */
    @PostMapping("/claims/{id}/handover/confirm")
    public ResponseEntity<ClaimResponse> confirmHandover(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.confirmHandover(user, id));
    }

    @PostMapping("/claims/{id}/complete")
    public ResponseEntity<ClaimResponse> complete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.completeHandover(user, id));
    }

    // ---------- the private thread ----------

    @GetMapping("/claims/{id}/messages")
    public ResponseEntity<List<ClaimMessageResponse>> messages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        requireUser(user);
        return ResponseEntity.ok(donationService.messages(user, id));
    }

    @PostMapping("/claims/{id}/messages")
    public ResponseEntity<ClaimMessageResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.sendMessage(user, id, body.get("body")));
    }

    // ---------- unread ----------

    /**
     * Drives the dot on the Exchanges link.
     *
     * Returns false rather than 401 for a signed-out visitor, because the
     * header asks for this on every page load and an error in the console on
     * every anonymous visit would be noise.
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Boolean>> unread(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(Map.of("unread", false));
        }
        return ResponseEntity.ok(Map.of("unread", donationService.hasUnread(user)));
    }

    /** Called when the exchanges page opens, which clears the dot. */
    @PostMapping("/seen")
    public ResponseEntity<Void> markSeen(@AuthenticationPrincipal User user) {
        requireUser(user);
        donationService.markExchangesSeen(user);
        return ResponseEntity.noContent().build();
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
    }
}
