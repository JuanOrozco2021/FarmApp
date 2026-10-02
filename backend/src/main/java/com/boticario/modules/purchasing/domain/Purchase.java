package com.boticario.modules.purchasing.domain;

import com.boticario.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de una compra a proveedor. Sus items solo se pueden agregar o
 * reemplazar mientras esta en BORRADOR (ver PurchaseService) — una vez
 * RECIBIDA, cada item ya quedo ligado a un movimiento real de kardex y
 * modificar la lista aqui rompería esa trazabilidad.
 */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
public class Purchase extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_code", nullable = false, unique = true, length = 20)
    private String purchaseCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status = PurchaseStatus.BORRADOR;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "received_by")
    private Long receivedBy;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(PurchaseItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
