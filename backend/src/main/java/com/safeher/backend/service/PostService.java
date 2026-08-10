package com.safeher.backend.service;

import com.safeher.backend.dto.CreatePostRequest;
import com.safeher.backend.dto.PageResponse;
import com.safeher.backend.dto.PostResponse;
import com.safeher.backend.entity.Category;
import com.safeher.backend.entity.Post;
import com.safeher.backend.entity.PostStatus;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.CategoryRepository;
import com.safeher.backend.repository.PostRepository;
import com.safeher.backend.spec.PostSpecifications;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final LocationCoarsener locationCoarsener;

    @Transactional
    public PostResponse create(User author, CreatePostRequest request) {
        Category category = categoryRepository
                .findBySlugIgnoreCase(request.categorySlug())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "That category does not exist. Pick one from the list."));

        BigDecimal lat = null;
        BigDecimal lng = null;

        // Both coordinates or neither — a lone latitude is meaningless.
        if (request.latitude() != null || request.longitude() != null) {
            if (!locationCoarsener.isValidLatitude(request.latitude())
                    || !locationCoarsener.isValidLongitude(request.longitude())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "That location does not look right. Leave it blank to post "
                        + "without a pin.");
            }
            lat = locationCoarsener.coarsen(request.latitude());
            lng = locationCoarsener.coarsen(request.longitude());
        }

        Post post = Post.builder()
                .author(author)
                .authorHandle(author.getAnonHandle())
                .category(category)
                .title(request.title().trim())
                .body(request.body().trim())
                .areaName(blankToNull(request.areaName()))
                .city(blankToNull(request.city()))
                .state(blankToNull(request.state()))
                .approxLat(lat)
                .approxLng(lng)
                .status(PostStatus.PUBLISHED)
                .build();

        return PostResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(
            String categorySlug, String city, String search,
            String sort, int page, int size) {

        Specification<Post> spec = PostSpecifications.visible();

        if (notBlank(categorySlug)) {
            spec = spec.and(PostSpecifications.inCategory(categorySlug.trim()));
        }
        if (notBlank(city)) {
            spec = spec.and(PostSpecifications.inCity(city.trim()));
        }
        if (notBlank(search)) {
            spec = spec.and(PostSpecifications.matching(search.trim()));
        }

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                resolveSort(sort));

        Page<Post> results = postRepository.findAll(spec, pageRequest);
        return PageResponse.of(results, PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse getById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That post no longer exists."));

        if (post.getStatus() == PostStatus.REMOVED
                || post.getStatus() == PostStatus.PENDING) {
            throw new ApiException(HttpStatus.NOT_FOUND, "That post no longer exists.");
        }

        return PostResponse.from(post);
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.toLowerCase()) {
            case "top" -> Sort.by(Sort.Direction.DESC, "upvoteCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "urgent" -> Sort.by(Sort.Direction.DESC, "urgencyScore")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }
}
