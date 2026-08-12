package com.safeher.backend.service;

import com.safeher.backend.dto.*;
import com.safeher.backend.entity.*;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.*;
import com.safeher.backend.spec.ListingSpecifications;
import com.safeher.backend.util.DonationHandleGenerator;
import com.safeher.backend.util.LocationCoarsener;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ListingRepository listingRepository;
    private final ListingClaimRepository claimRepository;
    private final ClaimMessageRepository messageRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final DonationHandleGenerator donationHandleGenerator;
    private final LocationCoarsener locationCoarsener;

    // ---------- listings ----------

    @Transactional
    public ListingResponse create(User user, CreateListingRequest request) {
        ItemType itemType = itemTypeRepository
                .findBySlugIgnoreCase(request.itemSlug())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "That item type does not exist. Pick one from the list."));

        Organisation organisation = null;
        HandledBy handledBy = request.handledBy() == null
                ? HandledBy.INDIVIDUAL : request.handledBy();

        if (handledBy == HandledBy.ORGANISATION) {
            if (request.organisationId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Choose which organisation is handling this.");
            }
            organisation = organisationRepository.findById(request.organisationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "That organisation is not in the directory."));
        }

        BigDecimal lat = null;
        BigDecimal lng = null;
        if (request.latitude() != null && request.longitude() != null) {
            lat = locationCoarsener.coarsen(request.latitude());
            lng = locationCoarsener.coarsen(request.longitude());
        }

        Listing listing = Listing.builder()
                .user(user)
                .userHandle(donationHandleFor(user))
                .listingType(request.listingType())
                .itemType(itemType)
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .quantity(blankToNull(request.quantity()))
                .areaName(blankToNull(request.areaName()))
                .city(blankToNull(request.city()))
                .approxLat(lat)
                .approxLng(lng)
                .detailHidden(Boolean.TRUE.equals(request.detailHidden()))
                .canSplit(Boolean.TRUE.equals(request.canSplit()))
                .handledBy(handledBy)
                .organisation(organisation)
                .status(ListingStatus.OPEN)
                .build();

        Listing saved = listingRepository.save(listing);
        return ListingResponse.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> list(
            String type, String item, String city, int page, int size) {

        Specification<Listing> spec = ListingSpecifications.visible();

        if (notBlank(type)) {
            spec = spec.and(ListingSpecifications.ofType(parseType(type)));
        }
        if (notBlank(item)) {
            spec = spec.and(ListingSpecifications.forItem(item.trim()));
        }
        if (notBlank(city)) {
            spec = spec.and(ListingSpecifications.inCity(city.trim()));
        }

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Listing> results = listingRepository.findAll(spec, pageRequest);

        return PageResponse.of(results, listing ->
                ListingResponse.from(listing, countClaims(listing)));
    }

    /**
     * A single listing.
     *
     * The owner, and anyone whose response has been accepted, see the full
     * description. Everyone else sees the redacted version when the listing
     * asked for that.
     */
    @Transactional(readOnly = true)
    public ListingResponse getById(UUID id, User viewer) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That listing no longer exists."));

        int claims = countClaims(listing);

        if (viewer != null && maySeeDetail(listing, viewer)) {
            return ListingResponse.full(listing, claims);
        }
        return ListingResponse.from(listing, claims);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine(User user) {
        return listingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(listing -> ListingResponse.full(listing, countClaims(listing)))
                .toList();
    }

    /** How a splittable listing gets closed: the donor says she has run out. */
    @Transactional
    public ListingResponse markFulfilled(User user, UUID listingId) {
        Listing listing = ownedListing(user, listingId);
        listing.setStatus(ListingStatus.FULFILLED);
        listingRepository.save(listing);
        return ListingResponse.full(listing, countClaims(listing));
    }

    // ---------- claims ----------

    /**
     * Responding to a listing.
     *
     * A previously declined or withdrawn response does not block a new one —
     * circumstances change, and someone turned down in March may be the right
     * person in June. Only a live response blocks another.
     */
    @Transactional
    public ClaimResponse claim(User user, UUID listingId, CreateClaimRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That listing no longer exists."));

        if (listing.getUser() != null && listing.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This is your own listing.");
        }

        if (listing.getStatus() == ListingStatus.FULFILLED
                || listing.getStatus() == ListingStatus.EXPIRED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This listing is closed.");
        }

        boolean hasLiveClaim = claimRepository
                .findByListingAndClaimantOrderByCreatedAtDesc(listing, user)
                .stream()
                .anyMatch(existing -> existing.getStatus() == ClaimStatus.PENDING
                        || existing.getStatus() == ClaimStatus.ACCEPTED);

        if (hasLiveClaim) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You have already responded to this one.");
        }

        ListingClaim claim = ListingClaim.builder()
                .listing(listing)
                .claimant(user)
                .message(blankToNull(request.message()))
                .status(ClaimStatus.PENDING)
                .build();

        claim = claimRepository.save(claim);

        systemMessage(claim, "Response sent. Waiting for a reply.");

        // A pending response deliberately does NOT take the listing off the
        // board. Only an accepted one does — otherwise a single unanswered
        // message would hide an offer from everyone else indefinitely.
        return ClaimResponse.from(claim, donationHandleFor(user), user.getId(), false);
    }

    /** Responses on the listings this person owns. */
    @Transactional(readOnly = true)
    public List<ClaimResponse> claimsOnMyListings(User user, UUID listingId) {
        Listing listing = ownedListing(user, listingId);

        return claimRepository.findByListingOrderByCreatedAtAsc(listing)
                .stream()
                .map(claim -> ClaimResponse.from(claim,
                        donationHandleFor(claim.getClaimant()), user.getId(), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> myClaims(User user) {
        return claimRepository.findByClaimantOrderByCreatedAtDesc(user)
                .stream()
                .map(claim -> ClaimResponse.from(claim,
                        donationHandleFor(user), user.getId(), false))
                .toList();
    }

    /**
     * The owner accepting or declining a response.
     *
     * Accepting normally takes the listing off the board, because the thing is
     * spoken for. A listing marked as splittable stays up — the donor may have
     * five packs and two people asking for two and three, and only she knows
     * when she has run out.
     *
     * Other pending responses are deliberately left alone rather than
     * auto-declined: if this arrangement falls through, the owner still has
     * people to turn to without anyone having to respond again.
     */
    @Transactional
    public ClaimResponse respondToClaim(User user, UUID claimId, boolean accept) {
        ListingClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That response no longer exists."));

        Listing listing = ownedListing(user, claim.getListing().getId());

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You have already answered this one.");
        }

        if (accept) {
            claim.setStatus(ClaimStatus.ACCEPTED);
            systemMessage(claim, "This response was accepted. "
                    + "Suggest a place and time below.");

            if (!listing.isCanSplit()) {
                listing.setStatus(ListingStatus.MATCHED);
                listingRepository.save(listing);
            }
        } else {
            claim.setStatus(ClaimStatus.DECLINED);
            systemMessage(claim, "This response was declined. "
                    + "You can respond again later if things change.");
        }

        claimRepository.save(claim);

        return ClaimResponse.from(claim,
                donationHandleFor(claim.getClaimant()), user.getId(), true);
    }

    /**
     * The claimant backing out.
     *
     * If theirs was the accepted response on a listing that is not splittable,
     * it goes straight back on the board — someone else may still want it, and
     * leaving it in limbo helps nobody. A splittable listing never came off the
     * board, so there is nothing to restore.
     */
    @Transactional
    public ClaimResponse withdrawClaim(User user, UUID claimId) {
        ListingClaim claim = claimRepository.findByIdAndClaimant(claimId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That response is not yours."));

        if (claim.getStatus() == ClaimStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This one is already done.");
        }

        boolean wasAccepted = claim.getStatus() == ClaimStatus.ACCEPTED;

        claim.setStatus(ClaimStatus.WITHDRAWN);
        claim.setWithdrawnAt(Instant.now());
        claim.setHandoverConfirmed(false);
        claimRepository.save(claim);

        systemMessage(claim, "This response was withdrawn.");

        if (wasAccepted && !claim.getListing().isCanSplit()) {
            Listing listing = claim.getListing();
            listing.setStatus(ListingStatus.OPEN);
            listingRepository.save(listing);
        }

        return ClaimResponse.from(claim, donationHandleFor(user), user.getId(), false);
    }

    // ---------- arranging the handover ----------

    /**
     * Proposing where and when to meet.
     *
     * Either side may propose, and whoever did is recorded so the other one is
     * asked to confirm. It is deliberately not the person giving who decides
     * alone: someone who has just posted that she cannot afford pads may not
     * have the fare to reach a place chosen for her, and has no say in whether
     * it feels safe.
     *
     * A new proposal clears any previous confirmation — changing the place
     * means agreeing again.
     */
    @Transactional
    public ClaimResponse proposeHandover(User user, UUID claimId, HandoverProposal proposal) {
        ListingClaim claim = accessibleClaim(user, claimId);

        if (claim.getStatus() != ClaimStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Arrange the handover once the response has been accepted.");
        }

        claim.setProposedPlace(proposal.place().trim());
        claim.setProposedTime(proposal.time().trim());
        claim.setProposedBy(user);
        claim.setProposedAt(Instant.now());
        claim.setHandoverConfirmed(false);
        claimRepository.save(claim);

        systemMessage(claim, donationHandleFor(user) + " suggested "
                + proposal.place().trim() + ", " + proposal.time().trim() + ".");

        return toClaimResponse(claim, user);
    }

    /** The other side agreeing to the proposal. */
    @Transactional
    public ClaimResponse confirmHandover(User user, UUID claimId) {
        ListingClaim claim = accessibleClaim(user, claimId);

        if (claim.getProposedPlace() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Nothing has been suggested yet.");
        }

        // Confirming your own suggestion means nothing.
        if (claim.getProposedBy() != null
                && claim.getProposedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Wait for the other person to confirm this.");
        }

        claim.setHandoverConfirmed(true);
        claimRepository.save(claim);

        systemMessage(claim, "Both of you have agreed on "
                + claim.getProposedPlace() + ", " + claim.getProposedTime() + ".");

        return toClaimResponse(claim, user);
    }

    /**
     * Either side marking it done.
     *
     * This closes an ordinary listing. A splittable one stays open — the donor
     * closes it herself once she has nothing left to give.
     */
    @Transactional
    public ClaimResponse completeHandover(User user, UUID claimId) {
        ListingClaim claim = accessibleClaim(user, claimId);

        claim.setStatus(ClaimStatus.COMPLETED);
        claimRepository.save(claim);

        systemMessage(claim, "Marked as handed over.");

        Listing listing = claim.getListing();
        if (!listing.isCanSplit()) {
            listing.setStatus(ListingStatus.FULFILLED);
            listingRepository.save(listing);
        }

        return toClaimResponse(claim, user);
    }

    // ---------- messages ----------

    @Transactional(readOnly = true)
    public List<ClaimMessageResponse> messages(User user, UUID claimId) {
        ListingClaim claim = accessibleClaim(user, claimId);

        return messageRepository.findByClaimOrderByCreatedAtAsc(claim)
                .stream()
                .map(ClaimMessageResponse::from)
                .toList();
    }

    @Transactional
    public ClaimMessageResponse sendMessage(User user, UUID claimId, String body) {
        ListingClaim claim = accessibleClaim(user, claimId);

        if (body == null || body.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Write something first.");
        }

        ClaimMessage message = ClaimMessage.builder()
                .claim(claim)
                .sender(user)
                .senderHandle(donationHandleFor(user))
                .body(body.trim())
                .system(false)
                .build();

        return ClaimMessageResponse.from(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<ItemTypeResponse> itemTypes() {
        return itemTypeRepository.findAllByOrderByIdAsc()
                .stream()
                .map(ItemTypeResponse::from)
                .toList();
    }

    // ---------- unread ----------

    /**
     * Whether anything has happened in this person's exchanges since they last
     * looked.
     *
     * Derived rather than stored: no notifications table, no read receipts,
     * just a timestamp on the user and a count of messages newer than it. It
     * answers "has anything happened", which is all a dot needs to answer.
     */
    @Transactional(readOnly = true)
    public boolean hasUnread(User user) {
        List<ListingClaim> involved = new ArrayList<>(
                claimRepository.findByClaimantOrderByCreatedAtDesc(user));

        listingRepository.findByUserOrderByCreatedAtDesc(user).forEach(listing ->
                involved.addAll(claimRepository.findByListingOrderByCreatedAtAsc(listing)));

        if (involved.isEmpty()) {
            return false;
        }

        // Never opened the page, but has exchanges: treat everything as new.
        if (user.getExchangesSeenAt() == null) {
            return true;
        }

        return messageRepository.countByClaimInAndCreatedAtAfter(
                involved, user.getExchangesSeenAt()) > 0;
    }

    /** Called when the exchanges page is opened. */
    @Transactional
    public void markExchangesSeen(User user) {
        user.setExchangesSeenAt(Instant.now());
        userRepository.save(user);
    }

    // ---------- helpers ----------

    /**
     * Records something that happened, as a message in the thread.
     *
     * Both people find out the same way they find out about anything else in
     * this exchange — by reading the conversation. No separate inbox, no
     * notification arriving without context.
     */
    private void systemMessage(ListingClaim claim, String text) {
        ClaimMessage message = ClaimMessage.builder()
                .claim(claim)
                .sender(null)
                .senderHandle("system")
                .body(text)
                .system(true)
                .build();

        messageRepository.save(message);
    }

    /**
     * Returns this user's donation handle, creating one on first use.
     *
     * Generated lazily rather than at registration, so accounts that never
     * touch the donation board never get a second identifier.
     */
    private String donationHandleFor(User user) {
        if (user.getDonationHandle() == null) {
            user.setDonationHandle(donationHandleGenerator.generateUnique());
            userRepository.save(user);
        }
        return user.getDonationHandle();
    }

    /** Builds a response from whichever side is looking at it. */
    private ClaimResponse toClaimResponse(ListingClaim claim, User viewer) {
        boolean isOwner = claim.getListing().getUser() != null
                && claim.getListing().getUser().getId().equals(viewer.getId());

        return ClaimResponse.from(claim,
                donationHandleFor(claim.getClaimant()), viewer.getId(), isOwner);
    }

    /**
     * Who may read a hidden description: the owner, and anyone whose response
     * has been accepted. Accepting is therefore what reveals the detail.
     */
    private boolean maySeeDetail(Listing listing, User viewer) {
        if (listing.getUser() != null && listing.getUser().getId().equals(viewer.getId())) {
            return true;
        }
        return claimRepository.findByListingOrderByCreatedAtAsc(listing)
                .stream()
                .anyMatch(claim -> claim.getClaimant().getId().equals(viewer.getId())
                        && claim.getStatus() == ClaimStatus.ACCEPTED);
    }

    private Listing ownedListing(User user, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That listing no longer exists."));

        if (listing.getUser() == null
                || !listing.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "That is not your listing.");
        }
        return listing;
    }

    /** Either side of a claim can read and write its thread. Nobody else can. */
    private ListingClaim accessibleClaim(User user, UUID claimId) {
        ListingClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That conversation no longer exists."));

        boolean isClaimant = claim.getClaimant().getId().equals(user.getId());
        boolean isOwner = claim.getListing().getUser() != null
                && claim.getListing().getUser().getId().equals(user.getId());

        if (!isClaimant && !isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "That conversation is not yours.");
        }
        return claim;
    }

    private int countClaims(Listing listing) {
        return claimRepository.findByListingOrderByCreatedAtAsc(listing).size();
    }

    private ListingType parseType(String type) {
        try {
            return ListingType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Use OFFER or REQUEST.");
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }
}
