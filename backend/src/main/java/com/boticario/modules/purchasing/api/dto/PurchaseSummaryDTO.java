package com.boticario.modules.purchasing.api.dto;

import com.boticario.modules.purchasing.domain.Purchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseSummaryDTO(
        Long id,
        String purchaseCode,
        Long supplierId,
        String supplierName,
        String invoiceNumber,
        LocalDate purchaseDate,
        String status,
        BigDecimal totalAmount
) {
    public static PurchaseSummaryDTO from(Purchase purchase) {
        return new PurchaseSummaryDTO(
                purchase.getId(), purchase.getPurchaseCode(),
                purchase.getSupplier().getId(), purchase.getSupplier().getName(),
                purchase.getInvoiceNumber(), purchase.getPurchaseDate(),
                purchase.getStatus().name(), purchase.getTotalAmount()
        );
    }
}
