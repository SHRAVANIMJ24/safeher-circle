package com.safeher.backend.dto;

import com.safeher.backend.entity.Category;

public record CategoryResponse(
        String slug,
        String label,
        String description,
        String icon
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getSlug(),
                category.getLabel(),
                category.getDescription(),
                category.getIcon());
    }
}
