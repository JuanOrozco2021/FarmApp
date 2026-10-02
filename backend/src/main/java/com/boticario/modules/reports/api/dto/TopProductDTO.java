package com.boticario.modules.reports.api.dto;

import java.math.BigDecimal;

public record TopProductDTO(
        Long productId,
        String productName,
        BigDecimal quantityBase,
        BigDecimal revenue
) {
}
