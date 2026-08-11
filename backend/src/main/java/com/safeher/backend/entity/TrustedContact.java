package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone who gets told when an alert is raised.
 *
 * Contacts are stored against the account, not the handle — this is the one
 * place where a real name and phone number are the point. They are never
 * exposed through any public endpoint.
 */
@Entity
@Table(name = "trusted_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "relationship", length = 50)
    private String relationship;

    /** 1 is contacted first. Lower numbers go earlier. */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Short priority = 1;

    @Column(name = "notify_by_sms", nullable = false)
    @Builder.Default
    private boolean notifyBySms = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
