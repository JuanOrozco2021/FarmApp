package com.boticario.modules.sales.domain;

import com.boticario.common.audit.Auditable;
import com.boticario.modules.customers.domain.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de una venta de mostrador. No existe estado "borrador": se
 * cobra en el momento (a diferencia de Purchase). Desde la Fase 7 una
 * venta puede ligarse a un Customer real (customer, nullable — sigue
 * habiendo ventas de mostrador anonimas); cuando se liga, customerName
 * queda como snapshot del nombre del cliente al momento de la venta (no
 * se reescribe si el cliente cambia de nombre despues, igual criterio de
 * inmutabilidad que el resto del kardex). Sin cliente, customerName sigue
 * siendo el texto libre que ya existia desde la Fase 6.
 */
@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
public class Sale extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_code", nullable = false, unique = true, length = 20)
    private String saleCode;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "change_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.ACTIVA;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_reason", length = 255)
    private String cancelledReason;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePayment> payments = new ArrayList<>();

    public BigDecimal totalPaid() {
        return payments.stream().map(SalePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
