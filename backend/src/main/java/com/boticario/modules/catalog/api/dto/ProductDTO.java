package com.boticario.modules.catalog.api.dto;

import com.boticario.modules.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDTO(
        Long id,
        String internalCode,
        String name,
        String activeIngredientName,
        String concentration,
        String pharmaceuticalForm,
        String categoryName,
        String laboratoryName,
        String brand,
        String description,
        String baseUnitCode,
        BigDecimal minStockBase,
        BigDecimal maxStockBase,
        boolean requiresPrescription,
        boolean fractionable,
        boolean controlsLot,
        boolean controlsExpiration,
        String status,
        List<PresentationDTO> presentations
) {
    public static ProductDTO from(Product product, List<PresentationDTO> presentations) {
        return new ProductDTO(
                product.getId(),
                product.getInternalCode(),
                product.getName(),
                product.getActiveIngredient() != null ? product.getActiveIngredient().getName() : null,
                product.getConcentration(),
                product.getPharmaceuticalForm(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getLaboratory() != null ? product.getLaboratory().getName() : null,
                product.getBrand(),
                product.getDescription(),
                product.getBaseUnit().getCode(),
                product.getMinStockBase(),
                product.getMaxStockBase(),
                product.isRequiresPrescription(),
                product.isFractionable(),
                product.isControlsLot(),
                product.isControlsExpiration(),
                product.getStatus().name(),
                presentations
        );
    }
}
