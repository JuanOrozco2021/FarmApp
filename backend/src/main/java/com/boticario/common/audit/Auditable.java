package com.boticario.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Superclase para toda entidad que deba registrar quien la creo/modifico
 * y cuando (regla obligatoria del encargo: trazabilidad de usuario).
 * No reemplaza la auditoria de acciones de negocio (audit_logs) — esa
 * vive en el modulo de auditoria y registra el detalle antes/despues.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 60)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 60)
    private String updatedBy;
}
