package com.safeher.backend.repository;

import com.safeher.backend.entity.Report;
import com.safeher.backend.entity.ReportStatus;
import com.safeher.backend.entity.TargetType;
import com.safeher.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Report> findByTargetTypeAndTargetId(TargetType targetType, UUID targetId);

    boolean existsByReporterAndTargetTypeAndTargetId(
            User reporter, TargetType targetType, UUID targetId);

    long countByTargetTypeAndTargetIdAndStatus(
            TargetType targetType, UUID targetId, ReportStatus status);
}
