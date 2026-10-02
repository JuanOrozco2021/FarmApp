package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.Sale;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleSummaryDTO(
        Long id,
        String saleCode,
        Long customerId,
        String customerName,
        BigDecimal total,
        String status,
        Instant createdAt
) {
    public static SaleSummaryDTO from(Sale sale) {
        return new SaleSummaryDTO(sale.getId(), sale.getSaleCode(),
                sale.getCustomer() != null ? sale.getCustomer().getId() : null, sale.getCustomerName(),
                sale.getTotal(), sale.getStatus().name(), sale.getCreatedAt());
    }
}
