package com.boticario.modules.inventory.domain;

import com.boticario.modules.catalog.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Existencia de un producto agrupada por lote. Cuando el producto no
 * controla lote (controlsLot=false), toda su existencia vive en un unico
 * lote sintetico de codigo GENERAL, para no bifurcar la logica de
 * StockAllocationService entre productos con y sin lote.
 */
@Entity
@Table(name = "inventory_lots")
@Getter
@Setter
@NoArgsConstructor
public class InventoryLot {

    public static final String GENERAL_LOT_CODE = "GENERAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "lot_code", nullable = false, length = 60)
    private String lotCode;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBase = BigDecimal.ZERO;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;
}
