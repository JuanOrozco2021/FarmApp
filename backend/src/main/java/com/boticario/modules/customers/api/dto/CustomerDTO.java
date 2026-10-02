package com.boticario.modules.customers.api.dto;

import com.boticario.modules.customers.domain.Customer;
import com.boticario.modules.customers.domain.DocumentType;

public record CustomerDTO(
        Long id,
        DocumentType documentType,
        String documentNumber,
        String name,
        String phone,
        String email,
        String address,
        boolean active
) {
    public static CustomerDTO from(Customer customer) {
        return new CustomerDTO(customer.getId(), customer.getDocumentType(), customer.getDocumentNumber(),
                customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress(), customer.isActive());
    }
}
