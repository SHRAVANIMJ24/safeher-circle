package com.safeher.backend.repository;

import com.safeher.backend.entity.ModerationScore;
import com.safeher.backend.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModerationScoreRepository extends JpaRepository<ModerationScore, Long> {

    Optional<ModerationScore> findFirstByTargetTypeAndTargetIdOrderByScoredAtDesc(
            TargetType targetType, UUID targetId);
}
