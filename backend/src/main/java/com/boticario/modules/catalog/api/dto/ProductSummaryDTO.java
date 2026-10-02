package com.boticario.modules.catalog.api.dto;

import com.boticario.modules.catalog.domain.Product;

import java.math.BigDecimal;

/** Version liviana para el listado paginado — no carga todas las presentaciones. */
public record ProductSummaryDTO(
        Long id,
        String internalCode,
        String name,
        String categoryName,
        String laboratoryName,
        String baseUnitCode,
        BigDecimal basePrice,
        String status,
        boolean requiresPrescription
) {
    public static ProductSummaryDTO from(Product product, BigDecimal basePrice) {
        return new ProductSummaryDTO(
                product.getId(),
                product.getInternalCode(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getLaboratory() != null ? product.getLaboratory().getName() : null,
                product.getBaseUnit().getCode(),
                basePrice,
                product.getStatus().name(),
                product.isRequiresPrescription()
        );
    }
}
