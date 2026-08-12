package com.safeher.backend.dto;

import com.safeher.backend.entity.HandledBy;
import com.safeher.backend.entity.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequest(

        @NotNull(message = "Say whether you are offering or asking")
        ListingType listingType,

        @NotBlank(message = "Choose what kind of item this is")
        String itemSlug,

        @NotBlank(message = "Give this a short title")
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String description,

        @Size(max = 100)
        String quantity,

        @Size(max = 150)
        String areaName,

        @Size(max = 100)
        String city,

        BigDecimal latitude,

        BigDecimal longitude,

        /** Requests can hide their description until someone makes contact. */
        Boolean detailHidden,
        

        HandledBy handledBy,

        /** Required when handledBy is ORGANISATION. */
        UUID organisationId,

        Boolean canSplit

        
) {}
