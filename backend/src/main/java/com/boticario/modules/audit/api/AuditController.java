package com.boticario.modules.audit.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.audit.api.dto.AuditLogDTO;
import com.boticario.modules.audit.application.AuditQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Consulta del historial de auditoria (Fase 10). AUDIT_VIEW ya existia
 * desde la semilla de la Fase 1 (V2__seed_identity_data.sql) pero nunca
 * se habia usado en ningun endpoint hasta ahora — solo lo tiene ADMIN.
 */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public PageResponse<AuditLogDTO> search(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {
        return auditQueryService.search(entityName, action, username, from, to, pageable);
    }
}
