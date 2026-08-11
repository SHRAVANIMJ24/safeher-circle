package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A helpline, NGO, shelter, legal aid clinic or police unit.
 *
 * {@code verified} matters more here than anywhere else in the app. A wrong
 * number in this table is worse than an empty table — someone in trouble
 * dials it, gets nothing, and loses time she may not have. Only rows checked
 * against the organisation's own published contact details are marked true.
 */
@Entity
@Table(name = "organisations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 30)
    private OrgType orgType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "alt_phone", length = 50)
    private String altPhone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** Null for national helplines, which are not tied to one city. */
    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "lat", precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", precision = 9, scale = 6)
    private BigDecimal lng;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "services", columnDefinition = "text[]")
    private List<String> services;

    @Column(name = "is_24x7", nullable = false)
    @Builder.Default
    private boolean available24x7 = false;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
