package com.safeher.backend.repository;

import com.safeher.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findBySlugIgnoreCase(String slug);

    List<Category> findAllByOrderBySortOrderAsc();
}
