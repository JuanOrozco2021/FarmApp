package com.boticario.modules.inventory.api.dto;

import java.math.BigDecimal;

public record StockDTO(
        Long productId,
        String internalCode,
        String productName,
        String baseUnitCode,
        BigDecimal quantityBase,
        BigDecimal minStockBase,
        BigDecimal maxStockBase,
        boolean lowStock
) {
}
