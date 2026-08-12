package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded by docs/schema.sql. Not user-extensible. */
@Entity
@Table(name = "item_types")
@Getter
@Setter
@NoArgsConstructor
public class ItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "slug", unique = true, nullable = false, length = 50)
    private String slug;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** Hygiene items are the ones this board exists for. */
    @Column(name = "is_hygiene", nullable = false)
    private boolean hygiene;
}
