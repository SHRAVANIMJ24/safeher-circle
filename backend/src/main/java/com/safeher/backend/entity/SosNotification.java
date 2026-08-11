package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A record of one attempt to reach one contact about one alert.
 *
 * Worth storing even when sending fails: after the fact, the useful question is
 * usually "did anyone actually get told?", and a failed SMS is a different
 * answer from no attempt at all.
 */
@Entity
@Table(name = "sos_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private SosAlert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private TrustedContact contact;

    /** SMS, PUSH or EMAIL. */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /** QUEUED, SENT, FAILED or ACKNOWLEDGED. */
    @Column(name = "delivery_status", nullable = false, length = 20)
    @Builder.Default
    private String deliveryStatus = "QUEUED";

    /** The provider's own id for the message, for chasing it up later. */
    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
