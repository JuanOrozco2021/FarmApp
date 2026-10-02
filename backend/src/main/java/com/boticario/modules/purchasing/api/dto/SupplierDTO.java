package com.boticario.modules.purchasing.api.dto;

import com.boticario.modules.purchasing.domain.Supplier;

public record SupplierDTO(
        Long id,
        String name,
        String taxId,
        String phone,
        String email,
        String address,
        String contactName,
        Integer paymentTermsDays,
        String notes,
        boolean active
) {
    public static SupplierDTO from(Supplier supplier) {
        return new SupplierDTO(supplier.getId(), supplier.getName(), supplier.getTaxId(), supplier.getPhone(),
                supplier.getEmail(), supplier.getAddress(), supplier.getContactName(), supplier.getPaymentTermsDays(),
                supplier.getNotes(), supplier.isActive());
    }
}
