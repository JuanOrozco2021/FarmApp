package com.boticario.modules.injections.domain;

import com.boticario.common.audit.Auditable;
import com.boticario.modules.customers.domain.Customer;
import com.boticario.modules.sales.domain.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registro de un servicio de inyectologia (Fase 8). Decision confirmada:
 * el medicamento SIEMPRE lo trae el paciente (o se compro aparte con una
 * venta normal del POS) — este modulo nunca toca inventory_stock ni
 * inventory_lots. medicationName/lotCode son texto libre, no referencian
 * el catalogo. relatedSale es solo una anotacion opcional para trazar
 * que el medicamento se compro en esta misma atencion, sin fusionar los
 * dos flujos. El cobro es una tarifa simple (fee + una sola forma de
 * pago), no el mecanismo de pagos divididos de Sale.
 */
@Entity
@Table(name = "injection_records")
@Getter
@Setter
@NoArgsConstructor
public class InjectionRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "injection_code", nullable = false, unique = true, length = 20)
    private String injectionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Snapshot del nombre del paciente: si hay customer, se copia de ahi; si no, es el texto libre informado. */
    @Column(name = "patient_name", nullable = false, length = 150)
    private String patientName;

    @Column(name = "medication_name", nullable = false, length = 150)
    private String medicationName;

    @Column(length = 60)
    private String dose;

    @Column(name = "lot_code", length = 60)
    private String lotCode;

    @Column(name = "application_site", length = 100)
    private String applicationSite;

    /** Solo una anotacion: no se valida contra el estado ni el cliente de esa venta. */
    @Column(name = "related_sale_id")
    private Long relatedSaleId;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(length = 500)
    private String observations;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InjectionStatus status = InjectionStatus.ACTIVA;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_reason", length = 255)
    private String cancelledReason;

    @Column(name = "cancelled_by")
    private Long cancelledBy;
}
