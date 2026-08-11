package com.safeher.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationDecision(

        /** REMOVE takes the content down. DISMISS leaves it up. */
        @NotNull(message = "Choose an action")
        Action action,

        @Size(max = 1000)
        String note
) {
    public enum Action {
        REMOVE,
        DISMISS
    }
}
