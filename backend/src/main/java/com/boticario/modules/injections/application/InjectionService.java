package com.boticario.modules.injections.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.audit.CurrentUserProvider;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.customers.domain.Customer;
import com.boticario.modules.customers.infrastructure.CustomerRepository;
import com.boticario.modules.injections.api.dto.CancelInjectionRequest;
import com.boticario.modules.injections.api.dto.InjectionRecordDTO;
import com.boticario.modules.injections.api.dto.InjectionRecordRequest;
import com.boticario.modules.injections.api.dto.InjectionRecordSummaryDTO;
import com.boticario.modules.injections.domain.InjectionRecord;
import com.boticario.modules.injections.domain.InjectionStatus;
import com.boticario.modules.injections.infrastructure.InjectionRecordRepository;
import com.boticario.modules.injections.infrastructure.InjectionRecordSpecifications;
import com.boticario.modules.sales.infrastructure.SaleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Servicio de inyectologia (Fase 8). Decision confirmada: nunca toca
 * inventario (el medicamento siempre lo trae el paciente); el cobro es
 * una tarifa simple, no el mecanismo de pagos divididos de Sale.
 */
@Service
public class InjectionService {

    private final InjectionRecordRepository injectionRecordRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final CurrentUserProvider currentUserProvider;

    public InjectionService(InjectionRecordRepository injectionRecordRepository,
                             CustomerRepository customerRepository,
                             SaleRepository saleRepository,
                             CurrentUserProvider currentUserProvider) {
        this.injectionRecordRepository = injectionRecordRepository;
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<InjectionRecordSummaryDTO> search(String search, InjectionStatus status, Long customerId, Pageable pageable) {
        Specification<InjectionRecord> spec = Specification.where(InjectionRecordSpecifications.codeOrPatientContains(search))
                .and(InjectionRecordSpecifications.hasStatus(status))
                .and(InjectionRecordSpecifications.belongsToCustomer(customerId));
        return PageResponse.from(injectionRecordRepository.findAll(spec, pageable).map(InjectionRecordSummaryDTO::from));
    }

    @Transactional(readOnly = true)
    public InjectionRecordDTO findById(Long id) {
        return InjectionRecordDTO.from(loadRecord(id));
    }

    @Transactional
    @Audited(action = "INJECTION_REGISTER", entity = "InjectionRecord")
    public InjectionRecordDTO create(InjectionRecordRequest request) {
        InjectionRecord record = new InjectionRecord();
        record.setInjectionCode(generateCode());
        applyPatient(record, request);

        if (request.relatedSaleId() != null && !saleRepository.existsById(request.relatedSaleId())) {
            throw new NotFoundException("Sale", request.relatedSaleId());
        }

        record.setMedicationName(request.medicationName().trim());
        record.setDose(blankToNull(request.dose()));
        record.setLotCode(blankToNull(request.lotCode()));
        record.setApplicationSite(blankToNull(request.applicationSite()));
        record.setRelatedSaleId(request.relatedSaleId());
        record.setObservations(blankToNull(request.observations()));
        record.setFee(request.fee());
        record.setPaymentMethod(request.paymentMethod());
        record.setAppliedAt(Instant.now());
        record.setAppliedBy(currentUserProvider.currentUserId().orElse(null));

        return InjectionRecordDTO.from(injectionRecordRepository.save(record));
    }

    @Transactional
    @Audited(action = "INJECTION_CANCEL", entity = "InjectionRecord")
    public InjectionRecordDTO cancel(Long id, CancelInjectionRequest request) {
        InjectionRecord record = loadRecord(id);
        if (record.getStatus() != InjectionStatus.ACTIVA) {
            throw new BusinessRuleException("Este registro ya fue anulado");
        }

        record.setStatus(InjectionStatus.ANULADA);
        record.setCancelledAt(Instant.now());
        record.setCancelledReason(request.reason());
        record.setCancelledBy(currentUserProvider.currentUserId().orElse(null));

        return InjectionRecordDTO.from(injectionRecordRepository.save(record));
    }

    /**
     * Si viene customerId, tiene prioridad: se valida que el cliente exista
     * y este activo, y su nombre se copia (snapshot) a patientName. Sin
     * customerId, patientName es obligatorio — a diferencia de una venta,
     * un servicio medico no puede quedar completamente anonimo.
     */
    private void applyPatient(InjectionRecord record, InjectionRecordRequest request) {
        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", request.customerId()));
            if (!customer.isActive()) {
                throw new BusinessRuleException("El cliente seleccionado esta inactivo");
            }
            record.setCustomer(customer);
            record.setPatientName(customer.getName());
        } else {
            String patientName = blankToNull(request.patientName());
            if (patientName == null) {
                throw new BusinessRuleException("El nombre del paciente es obligatorio si no se selecciona un cliente registrado");
            }
            record.setPatientName(patientName);
        }
    }

    private InjectionRecord loadRecord(Long id) {
        return injectionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("InjectionRecord", id));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String generateCode() {
        long seq = injectionRecordRepository.nextInjectionCodeSequence();
        return "INY-" + String.format("%06d", seq);
    }
}
