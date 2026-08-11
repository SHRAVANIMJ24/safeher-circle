package com.safeher.backend.dto;

import com.safeher.backend.entity.ReportReason;
import com.safeher.backend.entity.TargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReportRequest(

        @NotNull(message = "Say what is being reported")
        TargetType targetType,

        @NotNull(message = "Say what is being reported")
        UUID targetId,

        @NotNull(message = "Choose a reason")
        ReportReason reason,

        @Size(max = 2000)
        String detail
) {}
