package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.Sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleDTO(
        Long id,
        String saleCode,
        Long customerId,
        String customerName,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        BigDecimal changeAmount,
        String status,
        Instant createdAt,
        Instant cancelledAt,
        String cancelledReason,
        boolean hasPrescriptionItems,
        List<SaleItemDTO> items,
        List<SalePaymentDTO> payments
) {
    public static SaleDTO from(Sale sale) {
        List<SaleItemDTO> items = sale.getItems().stream().map(SaleItemDTO::from).toList();
        List<SalePaymentDTO> payments = sale.getPayments().stream().map(SalePaymentDTO::from).toList();
        boolean hasPrescriptionItems = sale.getItems().stream().anyMatch(i -> i.isRequiresPrescription());

        return new SaleDTO(
                sale.getId(), sale.getSaleCode(),
                sale.getCustomer() != null ? sale.getCustomer().getId() : null, sale.getCustomerName(),
                sale.getSubtotal(), sale.getDiscountTotal(), sale.getTotal(), sale.getChangeAmount(),
                sale.getStatus().name(), sale.getCreatedAt(), sale.getCancelledAt(), sale.getCancelledReason(),
                hasPrescriptionItems, items, payments
        );
    }
}
