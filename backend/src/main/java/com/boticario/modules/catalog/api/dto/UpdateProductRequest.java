package com.boticario.modules.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank String name,
        Long activeIngredientId,
        String concentration,
        String pharmaceuticalForm,
        Long categoryId,
        Long laboratoryId,
        String brand,
        String description,
        BigDecimal minStockBase,
        BigDecimal maxStockBase,
        boolean requiresPrescription,
        boolean fractionable,
        boolean controlsLot,
        boolean controlsExpiration
) {
}
