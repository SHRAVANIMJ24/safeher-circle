package com.safeher.backend.dto;

import jakarta.validation.constraints.Size;

public record CreateClaimRequest(
        @Size(max = 1000, message = "Keep the first message under 1,000 characters")
        String message
) {}
