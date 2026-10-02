package com.boticario.modules.purchasing.domain;

import com.boticario.modules.catalog.domain.ProductPresentation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una linea de compra: cantidad expresada en la presentacion comprada
 * (ej. 5 cajas) y ya convertida a unidad base (quantityBase) para no
 * recalcular en cada lectura ni depender de que el factor de conversion
 * de la presentacion no cambie despues. movementId solo se llena al
 * recibir la compra (antes de eso el item no ha tocado inventario).
 */
@Entity
@Table(name = "purchase_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private ProductPresentation presentation;

    @Column(name = "quantity_presentation", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityPresentation;

    @Column(name = "quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBase;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "lot_code", length = 60)
    private String lotCode;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "movement_id")
    private Long movementId;

    @Column(name = "returned_quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal returnedQuantityBase = BigDecimal.ZERO;

    /** Subtotal de la linea = cantidad comprada (en su presentacion) x costo unitario. */
    public BigDecimal lineTotal() {
        return quantityPresentation.multiply(unitCost);
    }
}
