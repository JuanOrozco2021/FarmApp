package com.boticario.modules.sales.domain;

import com.boticario.modules.catalog.domain.ProductPresentation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * unitPrice y requiresPrescription se copian de la presentacion/producto
 * AL MOMENTO de vender: si el precio o la regla de receta cambian despues,
 * el historial de esta venta no debe reescribirse (mismo criterio que
 * PurchaseItem.unitCost en la Fase 5).
 */
@Entity
@Table(name = "sale_items")
@Getter
@Setter
@NoArgsConstructor
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private ProductPresentation presentation;

    @Column(name = "quantity_presentation", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityPresentation;

    @Column(name = "quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBase;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription;

    @OneToMany(mappedBy = "saleItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItemLot> lots = new ArrayList<>();
}
