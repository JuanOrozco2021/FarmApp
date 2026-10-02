package com.boticario.modules.purchasing.domain;

import com.boticario.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dato maestro de proveedor. La Fase 5 dejo lo minimo (nombre, NIT,
 * contacto basico); la Fase 7 agrega persona de contacto, terminos de
 * pago informativos y notas. Deliberadamente NO se agregan cuentas por
 * pagar/saldo por factura: paymentTermsDays es solo un dato de referencia
 * (dias de credito pactados), no se valida ni se lleva saldo contra el —
 * es una funcionalidad completa aparte, igual criterio que el fiado de
 * clientes.
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
