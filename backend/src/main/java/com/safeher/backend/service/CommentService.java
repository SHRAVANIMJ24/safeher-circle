package com.safeher.backend.service;

import com.safeher.backend.dto.CommentResponse;
import com.safeher.backend.dto.CreateCommentRequest;
import com.safeher.backend.entity.Comment;
import com.safeher.backend.entity.ModerationScore;
import com.safeher.backend.entity.Post;
import com.safeher.backend.entity.PostStatus;
import com.safeher.backend.entity.TargetType;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.CommentRepository;
import com.safeher.backend.repository.ModerationScoreRepository;
import com.safeher.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final List<PostStatus> VISIBLE =
            List.of(PostStatus.PUBLISHED, PostStatus.FLAGGED);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ScoringClient scoringClient;
    private final ModerationScoreRepository moderationScoreRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> listForPost(UUID postId) {
        Post post = visiblePost(postId);

        List<Comment> all = commentRepository
                .findByPostAndStatusInOrderByCreatedAtAsc(post, VISIBLE);

        return buildTree(all, post.getAuthorHandle());
    }

    @Transactional
    public CommentResponse add(User author, UUID postId, CreateCommentRequest request) {
        Post post = visiblePost(postId);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "The comment you are replying to no longer exists."));

            if (!parent.getPost().getId().equals(post.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "That comment belongs to a different post.");
            }

            // Nesting stops at one level. Deeper threads are unreadable on a
            // phone, and this is a board people read on phones at night.
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .authorHandle(author.getAnonHandle())
                .body(request.body().trim())
                .parent(parent)
                .status(PostStatus.PUBLISHED)
                .build();

        comment = commentRepository.save(comment);
        recordScore(comment);

        // Kept on the post so the feed can show a count without a join.
        post.setCommentCount((int) commentRepository.countByPostAndStatusIn(post, VISIBLE));
        postRepository.save(post);

        return CommentResponse.from(comment, post.getAuthorHandle());
    }

    /**
     * Scores a reply for abusive language.
     *
     * This runs on comments rather than posts for a measured reason. Against
     * this platform's own content, toxic-bert scored an account of a sexual
     * assault at 0.0021 and a notice about free pads at 0.0006 — no usable
     * signal. It scored "you are a stupid liar... shut up" at 0.9851.
     *
     * The model reads abusive language aimed at a person, which is what
     * appears in replies, not what appears in posts.
     *
     * A high score sets FLAGGED, which raises the comment in the moderation
     * queue. FLAGGED content is still visible to everyone — see the VISIBLE
     * list above. Nothing is hidden without a person deciding.
     */
    private void recordScore(Comment comment) {
        scoringClient.score("", comment.getBody()).ifPresent(result -> {
            ModerationScore score = ModerationScore.builder()
                    .targetType(TargetType.COMMENT)
                    .targetId(comment.getId())
                    .toxicity(result.toxicity())
                    .urgency(result.urgency())
                    .predictedCategory(result.predictedCategory())
                    .modelVersion(result.modelVersion())
                    .autoAction(result.suggestedAction())
                    .build();

            moderationScoreRepository.save(score);

            if ("FLAG".equals(result.suggestedAction())) {
                comment.setStatus(PostStatus.FLAGGED);
                commentRepository.save(comment);
            }
        });
    }

    @Transactional
    public void remove(User user, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That comment no longer exists."));

        boolean isAuthor = comment.getAuthor() != null
                && comment.getAuthor().getId().equals(user.getId());
        boolean isModerator = user.getRole().name().equals("MODERATOR")
                || user.getRole().name().equals("ADMIN");

        if (!isAuthor && !isModerator) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You can only delete your own comments.");
        }

        // Soft delete: replies underneath would otherwise vanish with it.
        comment.setStatus(PostStatus.REMOVED);
        comment.setBody("[removed]");
        commentRepository.save(comment);

        Post post = comment.getPost();
        post.setCommentCount((int) commentRepository.countByPostAndStatusIn(post, VISIBLE));
        postRepository.save(post);
    }

    /**
     * Turns the flat, chronologically-ordered list into parents with replies.
     *
     * Because nesting is capped at one level, a single pass with a lookup map
     * is enough — no recursion needed.
     */
    private List<CommentResponse> buildTree(List<Comment> all, String postAuthorHandle) {
        Map<UUID, CommentResponse> topLevel = new LinkedHashMap<>();
        List<Comment> replies = new ArrayList<>();

        for (Comment comment : all) {
            if (comment.getParent() == null) {
                topLevel.put(comment.getId(), CommentResponse.from(comment, postAuthorHandle));
            } else {
                replies.add(comment);
            }
        }

        for (Comment reply : replies) {
            CommentResponse parent = topLevel.get(reply.getParent().getId());
            if (parent != null) {
                parent.replies().add(CommentResponse.from(reply, postAuthorHandle));
            }
        }

        return new ArrayList<>(topLevel.values());
    }

    private Post visiblePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That post no longer exists."));

        if (!VISIBLE.contains(post.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "That post no longer exists.");
        }

        return post;
    }
}
