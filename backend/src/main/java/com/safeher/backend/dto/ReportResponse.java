package com.safeher.backend.dto;

import com.safeher.backend.entity.Report;

import java.time.Instant;
import java.util.UUID;

/**
 * What a moderator sees.
 *
 * There is no reporter field. A moderator decides on the content, not on who
 * objected to it, and keeping the reporter out of the response means it cannot
 * leak through the API even by accident.
 *
 * The model's score is included as a raw number rather than a verdict. Showing
 * "0.98" invites a moderator to judge the model; showing "flagged by AI" invites
 * her to defer to it. Given the model scored an account of a sexual assault at
 * 0.0021, deferring to it would be a mistake.
 */
public record ReportResponse(
        UUID id,
        String targetType,
        UUID targetId,
        String reason,
        String detail,
        String status,
        Instant createdAt,

        /** Filled in for posts and comments so the queue is readable at a glance. */
        String contentTitle,
        String contentBody,
        String contentAuthorHandle,
        String contentStatus,
        long reportCount,

        /** Null when the scoring service was unavailable or never ran. */
        Float toxicityScore,
        String modelAction
) {
    public static ReportResponse of(
            Report report,
            String title,
            String body,
            String authorHandle,
            String contentStatus,
            long reportCount,
            Float toxicityScore,
            String modelAction) {

        return new ReportResponse(
                report.getId(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReason().name(),
                report.getDetail(),
                report.getStatus().name(),
                report.getCreatedAt(),
                title,
                body,
                authorHandle,
                contentStatus,
                reportCount,
                toxicityScore,
                modelAction);
    }
}
