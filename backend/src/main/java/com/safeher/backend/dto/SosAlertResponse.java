package com.safeher.backend.dto;

import com.safeher.backend.entity.SosAlert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SosAlertResponse(
        UUID id,
        String status,
        String triggerMethod,
        String alarmType,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant triggeredAt,
        Instant resolvedAt,
        int contactsNotified
) {
    public static SosAlertResponse from(SosAlert alert, int contactsNotified) {
        return new SosAlertResponse(
                alert.getId(),
                alert.getStatus().name(),
                alert.getTriggerMethod(),
                alert.getAlarmType(),
                alert.getExactLat(),
                alert.getExactLng(),
                alert.getTriggeredAt(),
                alert.getResolvedAt(),
                contactsNotified);
    }
}
