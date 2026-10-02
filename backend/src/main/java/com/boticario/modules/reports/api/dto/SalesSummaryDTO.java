package com.boticario.modules.reports.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesSummaryDTO(
        long totalSales,
        BigDecimal totalRevenue,
        BigDecimal totalDiscount,
        BigDecimal averageTicket,
        List<PaymentBreakdownDTO> paymentBreakdown
) {
}
