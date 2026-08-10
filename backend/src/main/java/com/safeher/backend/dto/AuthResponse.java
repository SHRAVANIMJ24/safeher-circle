package com.safeher.backend.dto;

import java.util.UUID;

/**
 * Returned on register and login. Note there is no email field — once
 * authenticated the client works with the handle, not the address.
 */
public record AuthResponse(
        String token,
        UUID userId,
        String anonHandle,
        String role,
        long expiresInSeconds
) {}
