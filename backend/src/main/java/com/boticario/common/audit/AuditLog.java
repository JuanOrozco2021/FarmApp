package com.boticario.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Fila de audit_logs (tabla creada en la V1__identity_schema.sql de la
 * Fase 1, nunca usada hasta la Fase 10). No extiende Auditable: esta tabla
 * ya tiene su propia columna de fecha (occurred_at) y nunca se edita ni se
 * borra una fila una vez escrita — no aplica created_by/updated_by.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 60)
    private String username;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_name", nullable = false, length = 60)
    private String entityName;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    /**
     * "Antes" = los argumentos de la llamada (la solicitud/intencion del
     * usuario), no una foto de la entidad en base de datos antes del
     * cambio — capturar eso de forma generica para escrituras de mas de
     * 25 metodos en 9 modulos distintos requeriria cargar cada entidad
     * dos veces (antes/despues) con su propio mapeo por modulo, lo que
     * dejaria de ser una unica pieza de infraestructura transversal.
     * Este antes/despues (solicitud/resultado) es un patron de auditoria
     * legitimo y suficiente para trazabilidad, y se documenta asi para no
     * insinuar una garantia que no existe — "el sistema nunca adivina",
     * tampoco en su propia documentacion.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb")
    private String beforeJson;

    /** "Despues" = el valor de retorno del metodo, tal como lo ve el cliente de la API. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb")
    private String afterJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
