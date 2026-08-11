package com.safeher.backend.repository;

import com.safeher.backend.entity.SosAlert;
import com.safeher.backend.entity.SosNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SosNotificationRepository extends JpaRepository<SosNotification, UUID> {

    List<SosNotification> findByAlert(SosAlert alert);
}
