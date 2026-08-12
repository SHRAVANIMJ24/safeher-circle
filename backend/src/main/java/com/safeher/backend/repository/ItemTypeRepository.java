package com.safeher.backend.repository;

import com.safeher.backend.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemTypeRepository extends JpaRepository<ItemType, Integer> {

    Optional<ItemType> findBySlugIgnoreCase(String slug);

    List<ItemType> findAllByOrderByIdAsc();
}
