package com.safeher.backend.repository;

import com.safeher.backend.entity.TrustedContact;
import com.safeher.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedContactRepository extends JpaRepository<TrustedContact, UUID> {

    List<TrustedContact> findByUserOrderByPriorityAsc(User user);

    long countByUser(User user);

    Optional<TrustedContact> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndPhone(User user, String phone);
}
