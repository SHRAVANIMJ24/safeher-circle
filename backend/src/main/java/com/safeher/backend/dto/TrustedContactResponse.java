package com.safeher.backend.dto;

import com.safeher.backend.entity.TrustedContact;

import java.util.UUID;

public record TrustedContactResponse(
        UUID id,
        String name,
        String phone,
        String relationship,
        short priority,
        boolean notifyBySms
) {
    public static TrustedContactResponse from(TrustedContact contact) {
        return new TrustedContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhone(),
                contact.getRelationship(),
                contact.getPriority(),
                contact.isNotifyBySms());
    }
}
