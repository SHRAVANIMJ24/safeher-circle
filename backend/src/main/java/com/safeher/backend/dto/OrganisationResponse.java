package com.safeher.backend.dto;

import com.safeher.backend.entity.Organisation;

import java.util.List;
import java.util.UUID;

public record OrganisationResponse(
        UUID id,
        String name,
        String orgType,
        String description,
        String phone,
        String altPhone,
        String email,
        String website,
        String address,
        String city,
        String state,
        List<String> services,
        boolean available24x7,
        boolean verified,
        /** True for national entries that apply everywhere. */
        boolean national
) {
    public static OrganisationResponse from(Organisation org) {
        return new OrganisationResponse(
                org.getId(),
                org.getName(),
                org.getOrgType().name(),
                org.getDescription(),
                org.getPhone(),
                org.getAltPhone(),
                org.getEmail(),
                org.getWebsite(),
                org.getAddress(),
                org.getCity(),
                org.getState(),
                org.getServices() == null ? List.of() : org.getServices(),
                org.isAvailable24x7(),
                org.isVerified(),
                org.getCity() == null);
    }
}
