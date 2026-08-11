package com.safeher.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommentRequest(

        @NotBlank(message = "Write something before posting")
        @Size(max = 5000, message = "Keep replies under 5,000 characters")
        String body,

        /** Omit for a top-level reply, or pass a comment id to reply to it. */
        UUID parentId
) {}
