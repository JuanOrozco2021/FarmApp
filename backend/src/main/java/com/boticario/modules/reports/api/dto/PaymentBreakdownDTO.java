package com.boticario.modules.reports.api.dto;

import java.math.BigDecimal;

public record PaymentBreakdownDTO(
        String paymentMethod,
        BigDecimal amount
) {
}
