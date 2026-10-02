package com.boticario.modules.customers.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.customers.api.dto.CustomerDTO;
import com.boticario.modules.customers.api.dto.CustomerRequest;
import com.boticario.modules.customers.domain.Customer;
import com.boticario.modules.customers.infrastructure.CustomerRepository;
import com.boticario.modules.customers.infrastructure.CustomerSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> search(String search, Boolean active, Pageable pageable) {
        Specification<Customer> spec = Specification.where(CustomerSpecifications.nameOrDocumentContains(search))
                .and(CustomerSpecifications.isActive(active));
        return PageResponse.from(customerRepository.findAll(spec, pageable).map(CustomerDTO::from));
    }

    @Transactional(readOnly = true)
    public CustomerDTO findById(Long id) {
        return CustomerDTO.from(loadCustomer(id));
    }

    @Transactional
    @Audited(action = "CUSTOMER_CREATE", entity = "Customer")
    public CustomerDTO create(CustomerRequest request) {
        validateDocument(request);
        validateDocumentNumberAvailable(request.documentNumber(), null);

        Customer customer = new Customer();
        applyRequest(customer, request);
        return CustomerDTO.from(customerRepository.save(customer));
    }

    @Transactional
    @Audited(action = "CUSTOMER_UPDATE", entity = "Customer")
    public CustomerDTO update(Long id, CustomerRequest request) {
        Customer customer = loadCustomer(id);
        validateDocument(request);
        validateDocumentNumberAvailable(request.documentNumber(), id);

        applyRequest(customer, request);
        return CustomerDTO.from(customerRepository.save(customer));
    }

    @Transactional
    @Audited(action = "CUSTOMER_STATUS_CHANGE", entity = "Customer")
    public CustomerDTO setActive(Long id, boolean active) {
        Customer customer = loadCustomer(id);
        // Nunca se borra fisicamente un cliente: las ventas ya registradas
        // siguen apuntando a el (regla 16 del encargo, igual que productos
        // y proveedores).
        customer.setActive(active);
        return CustomerDTO.from(customerRepository.save(customer));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setDocumentType(request.documentType());
        customer.setDocumentNumber(blankToNull(request.documentNumber()));
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
    }

    /** El sistema nunca adivina el tipo de documento: si hay numero, el tipo es obligatorio. */
    private void validateDocument(CustomerRequest request) {
        boolean hasNumber = blankToNull(request.documentNumber()) != null;
        if (hasNumber && request.documentType() == null) {
            throw new BusinessRuleException("Si informas un numero de documento, tambien debes indicar el tipo de documento");
        }
    }

    private void validateDocumentNumberAvailable(String documentNumber, Long currentId) {
        String normalized = blankToNull(documentNumber);
        if (normalized == null) {
            return;
        }
        customerRepository.findByDocumentNumber(normalized)
                .filter(c -> !c.getId().equals(currentId))
                .ifPresent(c -> {
                    throw new BusinessRuleException("Ya existe un cliente con el documento " + normalized);
                });
    }

    private Customer loadCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer", id));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
