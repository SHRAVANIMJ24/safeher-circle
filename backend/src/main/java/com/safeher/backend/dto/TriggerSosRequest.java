package com.safeher.backend.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Everything is optional on purpose. Someone triggering an alarm should never
 * be blocked because their phone could not get a GPS fix.
 */
public record TriggerSosRequest(

        BigDecimal latitude,

        BigDecimal longitude,

        Float accuracyMeters,

        /** BUTTON, VOICE, SHAKE or TIMER. Defaults to BUTTON. */
        @Size(max = 30)
        String triggerMethod,

        /** SIREN, SCREAM, MALE_VOICE or SILENT. */
        @Size(max = 30)
        String alarmType,

        @Size(max = 500)
        String note
) {}
