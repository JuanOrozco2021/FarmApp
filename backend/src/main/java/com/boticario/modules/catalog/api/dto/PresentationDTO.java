package com.boticario.modules.catalog.api.dto;

import com.boticario.modules.catalog.domain.ProductPresentation;

import java.math.BigDecimal;
import java.util.List;

public record PresentationDTO(
        Long id,
        String unitCode,
        String unitName,
        BigDecimal conversionFactor,
        BigDecimal salePrice,
        BigDecimal purchasePrice,
        boolean basePresentation,
        boolean active,
        List<BarcodeDTO> barcodes
) {
    public static PresentationDTO from(ProductPresentation presentation) {
        return new PresentationDTO(
                presentation.getId(),
                presentation.getUnit().getCode(),
                presentation.getUnit().getName(),
                presentation.getConversionFactor(),
                presentation.getSalePrice(),
                presentation.getPurchasePrice(),
                presentation.isBasePresentation(),
                presentation.isActive(),
                presentation.getBarcodes().stream()
                        .filter(com.boticario.modules.catalog.domain.Barcode::isActive)
                        .map(BarcodeDTO::from)
                        .toList()
        );
    }
}
