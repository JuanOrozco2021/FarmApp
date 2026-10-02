package com.boticario.modules.sales.domain;

import com.boticario.modules.inventory.domain.InventoryLot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Cuanto se tomo de un lote concreto para cubrir un SaleItem — un item
 * puede repartirse entre varios lotes porque FEFO asi lo decidio. Sin
 * esto, anular una venta no sabria a que lote reponer cada fraccion.
 */
@Entity
@Table(name = "sale_item_lots")
@Getter
@Setter
@NoArgsConstructor
public class SaleItemLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Column(name = "quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBase;

    @Column(name = "movement_id")
    private Long movementId;
}
