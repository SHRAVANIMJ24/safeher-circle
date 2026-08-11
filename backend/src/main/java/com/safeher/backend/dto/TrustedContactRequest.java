package com.safeher.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrustedContactRequest(

        @NotBlank(message = "Enter a name")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Enter a phone number")
        @Pattern(
                regexp = "^\\+?[0-9 \\-]{7,20}$",
                message = "Enter a phone number, digits only, with or without a country code")
        String phone,

        @Size(max = 50)
        String relationship,

        @Min(value = 1, message = "Priority starts at 1")
        @Max(value = 5, message = "Priority goes up to 5")
        Short priority,

        Boolean notifyBySms
) {}
