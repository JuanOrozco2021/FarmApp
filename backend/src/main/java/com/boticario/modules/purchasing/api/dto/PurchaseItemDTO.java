package com.boticario.modules.purchasing.api.dto;

import com.boticario.modules.purchasing.domain.PurchaseItem;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseItemDTO(
        Long id,
        Long presentationId,
        String productName,
        String unitCode,
        BigDecimal conversionFactor,
        BigDecimal quantityPresentation,
        BigDecimal quantityBase,
        BigDecimal unitCost,
        BigDecimal lineTotal,
        String lotCode,
        LocalDate expirationDate,
        Long movementId,
        BigDecimal returnedQuantityBase
) {
    public static PurchaseItemDTO from(PurchaseItem item) {
        var presentation = item.getPresentation();
        return new PurchaseItemDTO(
                item.getId(),
                presentation.getId(),
                presentation.getProduct().getName(),
                presentation.getUnit().getCode(),
                presentation.getConversionFactor(),
                item.getQuantityPresentation(),
                item.getQuantityBase(),
                item.getUnitCost(),
                item.lineTotal(),
                item.getLotCode(),
                item.getExpirationDate(),
                item.getMovementId(),
                item.getReturnedQuantityBase()
        );
    }
}
