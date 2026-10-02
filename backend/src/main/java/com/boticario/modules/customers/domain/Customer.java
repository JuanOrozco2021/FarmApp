package com.boticario.modules.customers.domain;

import com.boticario.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dato maestro MINIMO de cliente (Fase 7): nombre, documento y contacto,
 * lo suficiente para ligar una venta a un cliente real y ver su
 * historial de compras. Documento es opcional porque muchos clientes de
 * mostrador solo dan su nombre. Venta a credito/fiado con saldo pendiente
 * queda deliberadamente fuera de esta fase (ver V11__customers_and_
 * supplier_enrichment_schema.sql) — es una funcionalidad completa aparte.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", length = 30)
    private String documentNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private boolean active = true;
}
