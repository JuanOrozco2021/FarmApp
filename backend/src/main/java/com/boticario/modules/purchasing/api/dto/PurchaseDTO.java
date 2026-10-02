package com.boticario.modules.purchasing.api.dto;

import com.boticario.modules.purchasing.domain.Purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseDTO(
        Long id,
        String purchaseCode,
        Long supplierId,
        String supplierName,
        String invoiceNumber,
        LocalDate purchaseDate,
        String status,
        BigDecimal totalAmount,
        String notes,
        Instant receivedAt,
        List<PurchaseItemDTO> items
) {
    public static PurchaseDTO from(Purchase purchase) {
        List<PurchaseItemDTO> items = purchase.getItems().stream().map(PurchaseItemDTO::from).toList();
        return new PurchaseDTO(
                purchase.getId(), purchase.getPurchaseCode(),
                purchase.getSupplier().getId(), purchase.getSupplier().getName(),
                purchase.getInvoiceNumber(), purchase.getPurchaseDate(),
                purchase.getStatus().name(), purchase.getTotalAmount(), purchase.getNotes(),
                purchase.getReceivedAt(), items
        );
    }
}
