package com.safeher.backend.service;

import com.safeher.backend.dto.CreateReportRequest;
import com.safeher.backend.entity.*;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.CommentRepository;
import com.safeher.backend.repository.PostRepository;
import com.safeher.backend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    /**
     * How many separate people must report something before it is marked
     * FLAGGED.
     *
     * Flagged content stays visible — it only moves up the moderator queue.
     * Auto-hiding on a report count would hand any three coordinated accounts
     * the power to silence someone, and the people most likely to be targeted
     * that way are exactly the ones this site exists for.
     */
    private static final int FLAG_THRESHOLD = 3;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void report(User reporter, CreateReportRequest request) {
        requireTargetExists(request.targetType(), request.targetId());

        if (reportRepository.existsByReporterAndTargetTypeAndTargetId(
                reporter, request.targetType(), request.targetId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You have already reported this. A moderator will look at it.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(blankToNull(request.detail()))
                .status(ReportStatus.OPEN)
                .build();

        reportRepository.save(report);

        applyFlagThreshold(request.targetType(), request.targetId());
    }

    /** Marks content as FLAGGED once enough separate people have reported it. */
    private void applyFlagThreshold(TargetType targetType, java.util.UUID targetId) {
        long openReports = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                targetType, targetId, ReportStatus.OPEN);

        if (openReports < FLAG_THRESHOLD) {
            return;
        }

        if (targetType == TargetType.POST) {
            postRepository.findById(targetId).ifPresent(post -> {
                if (post.getStatus() == PostStatus.PUBLISHED) {
                    post.setStatus(PostStatus.FLAGGED);
                    postRepository.save(post);
                }
            });
        } else if (targetType == TargetType.COMMENT) {
            commentRepository.findById(targetId).ifPresent(comment -> {
                if (comment.getStatus() == PostStatus.PUBLISHED) {
                    comment.setStatus(PostStatus.FLAGGED);
                    commentRepository.save(comment);
                }
            });
        }
    }

    private void requireTargetExists(TargetType targetType, java.util.UUID targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            default -> true;
        };

        if (!exists) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "That content no longer exists.");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
