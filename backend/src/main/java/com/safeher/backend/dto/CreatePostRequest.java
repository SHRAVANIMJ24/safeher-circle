package com.safeher.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePostRequest(

        @NotBlank(message = "Give your post a title")
        @Size(max = 200, message = "Keep the title under 200 characters")
        String title,

        @NotBlank(message = "Write something in the body")
        @Size(max = 10000, message = "Keep the post under 10,000 characters")
        String body,

        @NotBlank(message = "Choose a category")
        String categorySlug,

        @Size(max = 150)
        String areaName,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        /** Optional. Rounded to roughly 1km before it is stored. */
        BigDecimal latitude,

        BigDecimal longitude
) {}
