package com.safeher.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Enter an email address")
        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Enter a password")
        @Size(min = 8, max = 100, message = "Use at least 8 characters")
        String password,

        @Size(max = 100)
        String displayCity
) {}
