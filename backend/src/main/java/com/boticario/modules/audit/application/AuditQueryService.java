package com.boticario.modules.audit.application;

import com.boticario.common.audit.AuditLog;
import com.boticario.common.audit.AuditLogRepository;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.audit.api.dto.AuditLogDTO;
import com.boticario.modules.audit.infrastructure.AuditLogSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Lado de lectura de la auditoria (Fase 10). No tiene dominio propio — al
 * igual que ReportService (Fase 9), agrega/consulta datos que otro
 * componente ya escribio (aqui, AuditAspect via AuditLogRepository).
 */
@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> search(String entityName, String action, String username,
                                             Instant from, Instant to, Pageable pageable) {
        Specification<AuditLog> spec = Specification.where(AuditLogSpecifications.hasEntityName(entityName))
                .and(AuditLogSpecifications.hasAction(action))
                .and(AuditLogSpecifications.hasUsername(username))
                .and(AuditLogSpecifications.occurredBetween(from, to));

        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "occurredAt"));

        return PageResponse.from(auditLogRepository.findAll(spec, sorted).map(AuditLogDTO::from));
    }
}
