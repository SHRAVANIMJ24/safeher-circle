package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A raised alarm.
 *
 * Note the coordinates here are exact, unlike a post's. Posts are coarsened
 * because precision endangers the author; an alert is the opposite case, where
 * precision is the entire point. Same app, opposite defaults, on purpose.
 */
@Entity
@Table(name = "sos_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SosStatus status = SosStatus.ACTIVE;

    /** BUTTON, VOICE, SHAKE or TIMER. */
    @Column(name = "trigger_method", length = 30)
    private String triggerMethod;

    /** SIREN, SCREAM, MALE_VOICE or SILENT. */
    @Column(name = "alarm_type", length = 30)
    private String alarmType;

    @Column(name = "exact_lat", precision = 9, scale = 6)
    private BigDecimal exactLat;

    @Column(name = "exact_lng", precision = 9, scale = 6)
    private BigDecimal exactLng;

    @Column(name = "accuracy_meters")
    private Float accuracyMeters;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @PrePersist
    void onCreate() {
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }
}
