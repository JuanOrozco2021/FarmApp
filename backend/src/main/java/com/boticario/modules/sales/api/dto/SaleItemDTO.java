package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.SaleItem;

import java.math.BigDecimal;
import java.util.List;

public record SaleItemDTO(
        Long id,
        Long presentationId,
        String productName,
        String unitCode,
        BigDecimal conversionFactor,
        BigDecimal quantityPresentation,
        BigDecimal quantityBase,
        BigDecimal unitPrice,
        BigDecimal discountPercentage,
        BigDecimal lineTotal,
        boolean requiresPrescription,
        List<SaleItemLotInfo> lots
) {
    public static SaleItemDTO from(SaleItem item) {
        var presentation = item.getPresentation();
        List<SaleItemLotInfo> lots = item.getLots().stream().map(SaleItemLotInfo::from).toList();
        return new SaleItemDTO(
                item.getId(), presentation.getId(), presentation.getProduct().getName(),
                presentation.getUnit().getCode(), presentation.getConversionFactor(),
                item.getQuantityPresentation(), item.getQuantityBase(), item.getUnitPrice(),
                item.getDiscountPercentage(), item.getLineTotal(), item.isRequiresPrescription(), lots
        );
    }
}
