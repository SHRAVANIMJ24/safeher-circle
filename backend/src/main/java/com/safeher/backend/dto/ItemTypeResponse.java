package com.safeher.backend.dto;

import com.safeher.backend.entity.ItemType;

public record ItemTypeResponse(String slug, String label, boolean hygiene) {

    public static ItemTypeResponse from(ItemType type) {
        return new ItemTypeResponse(type.getSlug(), type.getLabel(), type.isHygiene());
    }
}
