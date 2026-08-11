package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * What the scoring service said about a piece of content.
 *
 * Kept as a record rather than folded into the post, because the useful
 * question later is "what did the model think, and was it right?" — and that
 * needs the score preserved next to the moderator's eventual decision.
 */
@Entity
@Table(name = "moderation_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "toxicity")
    private Float toxicity;

    @Column(name = "urgency")
    private Float urgency;

    @Column(name = "predicted_category", length = 50)
    private String predictedCategory;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /** NONE, REVIEW or FLAG. Advisory — nothing is hidden on this basis. */
    @Column(name = "auto_action", length = 20)
    private String autoAction;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @PrePersist
    void onCreate() {
        if (scoredAt == null) {
            scoredAt = Instant.now();
        }
    }
}
