package com.boticario.modules.purchasing.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.purchasing.api.dto.SupplierDTO;
import com.boticario.modules.purchasing.api.dto.SupplierRequest;
import com.boticario.modules.purchasing.domain.Supplier;
import com.boticario.modules.purchasing.infrastructure.SupplierRepository;
import com.boticario.modules.purchasing.infrastructure.SupplierSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierDTO> search(String search, Boolean active, Pageable pageable) {
        Specification<Supplier> spec = Specification.where(SupplierSpecifications.nameOrTaxIdContains(search))
                .and(SupplierSpecifications.isActive(active));
        return PageResponse.from(supplierRepository.findAll(spec, pageable).map(SupplierDTO::from));
    }

    @Transactional(readOnly = true)
    public SupplierDTO findById(Long id) {
        return SupplierDTO.from(supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier", id)));
    }

    @Transactional
    @Audited(action = "SUPPLIER_CREATE", entity = "Supplier")
    public SupplierDTO create(SupplierRequest request) {
        validateTaxIdAvailable(request.taxId(), null);

        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        return SupplierDTO.from(supplierRepository.save(supplier));
    }

    @Transactional
    @Audited(action = "SUPPLIER_UPDATE", entity = "Supplier")
    public SupplierDTO update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier", id));
        validateTaxIdAvailable(request.taxId(), id);

        applyRequest(supplier, request);
        return SupplierDTO.from(supplierRepository.save(supplier));
    }

    @Transactional
    @Audited(action = "SUPPLIER_STATUS_CHANGE", entity = "Supplier")
    public SupplierDTO setActive(Long id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier", id));
        // Nunca se borra fisicamente un proveedor: las compras ya registradas
        // siguen apuntando a el (regla 16 del encargo, igual que productos).
        supplier.setActive(active);
        return SupplierDTO.from(supplierRepository.save(supplier));
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name());
        supplier.setTaxId(blankToNull(request.taxId()));
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setContactName(request.contactName());
        supplier.setPaymentTermsDays(request.paymentTermsDays());
        supplier.setNotes(request.notes());
    }

    private void validateTaxIdAvailable(String taxId, Long currentId) {
        String normalized = blankToNull(taxId);
        if (normalized == null) {
            return;
        }
        supplierRepository.findByTaxId(normalized)
                .filter(s -> !s.getId().equals(currentId))
                .ifPresent(s -> {
                    throw new BusinessRuleException("Ya existe un proveedor con el NIT/RUC " + normalized);
                });
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
