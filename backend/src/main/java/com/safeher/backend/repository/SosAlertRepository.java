package com.safeher.backend.repository;

import com.safeher.backend.entity.SosAlert;
import com.safeher.backend.entity.SosStatus;
import com.safeher.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    Optional<SosAlert> findFirstByUserAndStatusOrderByTriggeredAtDesc(
            User user, SosStatus status);

    List<SosAlert> findByUserOrderByTriggeredAtDesc(User user);

    Optional<SosAlert> findByIdAndUser(UUID id, User user);
}
