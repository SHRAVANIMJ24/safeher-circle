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
        long reportCount
) {
    public static ReportResponse of(
            Report report,
            String title,
            String body,
            String authorHandle,
            String contentStatus,
            long reportCount) {

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
                reportCount);
    }
}
