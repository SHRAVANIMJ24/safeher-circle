package com.safeher.backend.spec;

import com.safeher.backend.entity.Listing;
import com.safeher.backend.entity.ListingStatus;
import com.safeher.backend.entity.ListingType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    /** Fulfilled and expired listings drop off the board. */
    public static Specification<Listing> visible() {
        return (root, query, cb) -> root.get("status").in(
                List.of(ListingStatus.OPEN, ListingStatus.MATCHED));
    }

    public static Specification<Listing> ofType(ListingType type) {
        return (root, query, cb) -> cb.equal(root.get("listingType"), type);
    }

    public static Specification<Listing> forItem(String slug) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("itemType").get("slug")), slug.toLowerCase());
    }

    public static Specification<Listing> inCity(String city) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }
}
