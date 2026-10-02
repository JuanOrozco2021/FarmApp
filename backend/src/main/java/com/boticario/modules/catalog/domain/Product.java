package com.boticario.modules.catalog.domain;

import com.boticario.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * El producto NUNCA guarda precio ni codigo de barras propios: esos viven
 * en sus presentaciones (ProductPresentation/Barcode). Todo el inventario
 * de este producto se mueve, compara y audita en baseUnit (regla central
 * del encargo).
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_code", nullable = false, unique = true, length = 20)
    private String internalCode;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_ingredient_id")
    private ActiveIngredient activeIngredient;

    @Column(length = 60)
    private String concentration;

    @Column(name = "pharmaceutical_form", length = 60)
    private String pharmaceuticalForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    @Column(length = 100)
    private String brand;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_unit_id", nullable = false)
    private Unit baseUnit;

    @Column(name = "min_stock_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal minStockBase = BigDecimal.ZERO;

    @Column(name = "max_stock_base", precision = 14, scale = 3)
    private BigDecimal maxStockBase;

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription = false;

    @Column(name = "is_fractionable", nullable = false)
    private boolean fractionable = true;

    @Column(name = "controls_lot", nullable = false)
    private boolean controlsLot = true;

    @Column(name = "controls_expiration", nullable = false)
    private boolean controlsExpiration = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;
}
