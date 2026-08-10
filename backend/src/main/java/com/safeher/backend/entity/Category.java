package com.safeher.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A topic a post can be filed under. Rows are seeded by docs/schema.sql rather
 * than created at runtime — the list is a deliberate editorial choice, not
 * something users extend.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Stable identifier used in URLs, e.g. "mental-health". */
    @Column(name = "slug", unique = true, nullable = false, length = 50)
    private String slug;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}
