package com.safeher.backend.service;

import com.safeher.backend.dto.ModerationDecision;
import com.safeher.backend.dto.PageResponse;
import com.safeher.backend.dto.ReportResponse;
import com.safeher.backend.entity.*;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.CommentRepository;
import com.safeher.backend.repository.PostRepository;
import com.safeher.backend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The human review side.
 *
 * Nothing in this service is automatic. A model or a report count can move
 * something into this queue; only a person decides what happens to it. That
 * separation is the whole point — a classifier cannot tell someone describing
 * harassment apart from someone committing it, because the words are the same.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> queue(String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0), Math.clamp(size, 1, 50));

        Page<Report> reports;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            reports = reportRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        } else {
            ReportStatus parsed = parseStatus(status);
            reports = reportRepository.findByStatusOrderByCreatedAtAsc(parsed, pageRequest);
        }

        return PageResponse.of(reports, this::withContent);
    }

    @Transactional
    public ReportResponse decide(User moderator, UUID reportId, ModerationDecision decision) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That report does not exist."));

        if (report.getStatus() == ReportStatus.ACTIONED
                || report.getStatus() == ReportStatus.DISMISSED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Another moderator has already handled this one.");
        }

        if (decision.action() == ModerationDecision.Action.REMOVE) {
            takeDown(report);
            report.setStatus(ReportStatus.ACTIONED);
        } else {
            restore(report);
            report.setStatus(ReportStatus.DISMISSED);
        }

        report.setReviewedBy(moderator);
        report.setReviewedAt(Instant.now());
        reportRepository.save(report);

        return withContent(report);
    }

    /**
     * Content is hidden, not deleted.
     *
     * Keeping the row means a wrong decision can be undone, and means there is
     * still a record if the same account is reported again later.
     */
    private void takeDown(Report report) {
        if (report.getTargetType() == TargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(post -> {
                post.setStatus(PostStatus.REMOVED);
                postRepository.save(post);
            });
        } else if (report.getTargetType() == TargetType.COMMENT) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
                comment.setStatus(PostStatus.REMOVED);
                commentRepository.save(comment);
            });
        }
    }

    /** Puts flagged content back to normal when a report is dismissed. */
    private void restore(Report report) {
        if (report.getTargetType() == TargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(post -> {
                if (post.getStatus() == PostStatus.FLAGGED) {
                    post.setStatus(PostStatus.PUBLISHED);
                    postRepository.save(post);
                }
            });
        } else if (report.getTargetType() == TargetType.COMMENT) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
                if (comment.getStatus() == PostStatus.FLAGGED) {
                    comment.setStatus(PostStatus.PUBLISHED);
                    commentRepository.save(comment);
                }
            });
        }
    }

    /** Attaches the reported content so a moderator does not have to go hunting. */
    private ReportResponse withContent(Report report) {
        long count = reportRepository
                .findByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId())
                .size();

        if (report.getTargetType() == TargetType.POST) {
            return postRepository.findById(report.getTargetId())
                    .map(post -> ReportResponse.of(report, post.getTitle(), post.getBody(),
                            post.getAuthorHandle(), post.getStatus().name(), count))
                    .orElseGet(() -> ReportResponse.of(report, null,
                            "[content no longer exists]", null, "GONE", count));
        }

        if (report.getTargetType() == TargetType.COMMENT) {
            return commentRepository.findById(report.getTargetId())
                    .map(comment -> ReportResponse.of(report, null, comment.getBody(),
                            comment.getAuthorHandle(), comment.getStatus().name(), count))
                    .orElseGet(() -> ReportResponse.of(report, null,
                            "[content no longer exists]", null, "GONE", count));
        }

        return ReportResponse.of(report, null, null, null, null, count);
    }

    private ReportStatus parseStatus(String status) {
        try {
            return ReportStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unknown status. Use OPEN, REVIEWING, ACTIONED, DISMISSED or ALL.");
        }
    }
}
