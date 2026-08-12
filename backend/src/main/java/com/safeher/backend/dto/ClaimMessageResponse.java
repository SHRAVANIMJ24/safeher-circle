package com.safeher.backend.dto;

import com.safeher.backend.entity.ClaimMessage;

import java.time.Instant;
import java.util.UUID;

public record ClaimMessageResponse(
        UUID id,
        String senderHandle,
        String body,
        boolean system,
        Instant createdAt
) {
    public static ClaimMessageResponse from(ClaimMessage message) {
        return new ClaimMessageResponse(
                message.getId(),
                message.getSenderHandle(),
                message.getBody(),
                message.isSystem(),
                message.getCreatedAt());
    }
}
