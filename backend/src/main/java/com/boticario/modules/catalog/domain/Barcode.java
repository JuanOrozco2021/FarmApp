package com.boticario.modules.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Codigo de barras impreso en un empaque, asociado a UNA presentacion
 * concreta (nunca directamente al producto). Un mismo producto puede tener
 * un codigo distinto por cada presentacion (unidad, blister, caja).
 */
@Entity
@Table(name = "barcodes")
@Getter
@Setter
@NoArgsConstructor
public class Barcode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private ProductPresentation presentation;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
