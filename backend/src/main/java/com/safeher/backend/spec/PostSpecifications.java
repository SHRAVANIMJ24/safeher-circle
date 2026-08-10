package com.safeher.backend.spec;

import com.safeher.backend.entity.Post;
import com.safeher.backend.entity.PostStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Reusable query fragments. Each one is optional except {@link #visible()};
 * the service chains together whichever the request actually asked for.
 */
public final class PostSpecifications {

    private PostSpecifications() {
    }

    /** Removed posts never appear on public endpoints, filtered or not. */
    public static Specification<Post> visible() {
        return (root, query, cb) -> root.get("status").in(
                List.of(PostStatus.PUBLISHED, PostStatus.FLAGGED));
    }

    public static Specification<Post> inCategory(String slug) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category").get("slug")), slug.toLowerCase());
    }

    public static Specification<Post> inCity(String city) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    /** Matches the term anywhere in the title or body, case-insensitively. */
    public static Specification<Post> matching(String term) {
        return (root, query, cb) -> {
            String pattern = "%" + term.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("body")), pattern));
        };
    }
}
