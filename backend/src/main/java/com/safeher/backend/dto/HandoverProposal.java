package com.safeher.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HandoverProposal(

        @NotBlank(message = "Suggest somewhere public")
        @Size(max = 300)
        String place,

        @NotBlank(message = "Suggest a time")
        @Size(max = 120)
        String time
) {}
