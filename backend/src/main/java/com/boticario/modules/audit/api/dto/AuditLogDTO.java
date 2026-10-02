package com.boticario.modules.audit.api.dto;

import com.boticario.common.audit.AuditLog;

import java.time.Instant;

public record AuditLogDTO(
        Long id,
        Long userId,
        String username,
        String action,
        String entityName,
        String entityId,
        String beforeJson,
        String afterJson,
        Instant occurredAt
) {
    public static AuditLogDTO from(AuditLog log) {
        return new AuditLogDTO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getBeforeJson(),
                log.getAfterJson(),
                log.getOccurredAt()
        );
    }
}
